# Simple-RPC Showcase

[Simple-RPC](https://github.com/naivetoby/simple-rpc) 的微服务示例，使用 Java 21、Spring Boot 4.2.0 和 Simple-RPC 3.5.0。HTTP 网关通过 RabbitMQ 向独立的话题服务追加事件。通过查询同一个话题的事件列表，可以比较同步、异步和延迟调用的结果，以及按话题分区后的消费顺序。

## 快速开始

在仓库根目录使用 Docker Compose v2 启动三个容器：`gateway`、`topic-service` 和 `rabbitmq`。两种模式只改变应用镜像；网关和话题服务都通过容器网络连接 `rabbitmq:5672`，宿主机只开放网关 `127.0.0.1:8080`。

### JVM（默认）

```sh
docker compose up -d --build --wait
```

### GraalVM Native Image

选择 `native` 镜像目标。Docker 在 Linux 构建阶段使用 GraalVM JDK 25 编译网关和话题服务，再分别运行两个原生程序：

```sh
APP_MODE=native docker compose up -d --build --wait
```

两种模式择一启动。切换模式前执行 `docker compose down -v`，让下一轮测试从空队列开始。

### 验证调用

网关监听 `127.0.0.1:8080`。服务启动后，向新话题同步追加一个事件，再读取已消费的事件列表：

```sh
url="http://127.0.0.1:8080/topics/demo-$(date +%s)/events"
curl -i -X POST "$url?eventId=first"   # HTTP 200
curl -s "$url"                         # ["first"]
```

再提交一个延迟 3 秒的事件。这次网关立即返回 `202`，事件要等服务端消费后才会出现在列表中：

```sh
curl -i -X POST "$url?eventId=later&mode=delay&delayMs=3000"
# 稍后再次查询
curl -s "$url"                         # 消费完成后：["first","later"]
```

运行完整用例见[构建与测试](#构建与测试)。测试结束后，清理容器、网络和数据卷：

```sh
docker compose down -v
```

## 接口与调用模式

- `GET /topics/{topic}/events`：同步查询已消费的事件 ID，空话题返回 `[]`。
- `POST /topics/{topic}/events`：通过查询参数 `eventId` 指定事件，`mode` 选择投递方式。延迟模式下可指定 `delayMs`（默认 `1000`，范围 `1`–`60000` 毫秒）。

| `mode` | 投递方式 | 成功响应 |
| --- | --- | --- |
| `sync`（默认） | 同步追加，等待服务端结果 | `200` |
| `async` | 提交到异步主队列 | `202` |
| `ordered` | 按 `topic` 投递到异步分区队列 | `202` |
| `delay` | 延迟投递到主队列 | `202` |
| `delayedOrdered` | 延迟投递到 `topic` 对应的分区队列 | `202` |

`202` 只表示已提交，不是消费回执。分区顺序指**同一队列的入队顺序**，不保证跨队列的全局顺序，也不保证延迟消息按发送顺序消费。

服务端按 `(topic, eventId)` 去重。例如，使用默认同步模式重复提交 `first` 会得到 `409`，提交空 `eventId` 会得到 `400`。事件列表和去重记录均保存在单实例内存中，重启后清空。

## 项目结构

项目采用 Maven 多模块布局。`gateway` 和 `topic-service` 是独立的 Spring Boot 应用，只共享 `contracts`，不依赖对方的实现。

| 模块 | 职责 | 源码入口 |
| --- | --- | --- |
| `contracts` | 声明 RPC 客户端、方法别名、分区键和请求 DTO | [客户端接口](contracts/src/main/java/showcase/contracts/client/topic) · [TopicEventDTO](contracts/src/main/java/showcase/contracts/entity/topic/TopicEventDTO.java) |
| `gateway` | 接收 HTTP 请求，调用 RPC 接口并转换响应状态 | [TopicController](gateway/src/main/java/showcase/gateway/controller/TopicController.java) · [客户端注册](gateway/src/main/java/showcase/gateway/config/RpcClientConfig.java) |
| `topic-service` | 校验请求、消费事件并提供同步查询 | [TopicServer](topic-service/src/main/java/showcase/topic/server/TopicServer.java) · [去重处理](topic-service/src/main/java/showcase/topic/handler/TopicDuplicateHandler.java) |

## 构建与测试

使用 JDK 21 和 Maven 在本地构建全部模块：

```sh
mvn clean package
```

启动 JVM 或 Native 双端后，使用 Python 3 运行同一套[端到端冒烟测试](scripts/smoke.py)：

```sh
python3 scripts/smoke.py
```

脚本会等待服务就绪，并用独立话题验证五种投递方式、参数校验及重复请求。全部通过时输出：

```text
OK: sync, validation, duplicate, async, partition order, delay and partitioned delay
```

可通过 `SHOWCASE_URL` 指定网关地址。使用 `docker compose logs gateway topic-service rabbitmq` 查看应用和消息队列日志。
