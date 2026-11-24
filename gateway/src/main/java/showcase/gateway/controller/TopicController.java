package showcase.gateway.controller;

import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import showcase.contracts.client.topic.TopicAsyncClient;
import showcase.contracts.client.topic.TopicClient;
import showcase.contracts.client.topic.TopicDelayClient;
import showcase.contracts.entity.topic.TopicEventDTO;
import vip.toby.rpc.entity.RCode;
import vip.toby.rpc.entity.RpcResult;

@RestController
@RequestMapping("/topics/{topic}/events")
@RequiredArgsConstructor
public class TopicController {

    private final TopicClient topicClient;
    private final TopicAsyncClient topicAsyncClient;
    private final TopicDelayClient topicDelayClient;

    /**
     * 获取话题下已消费的事件列表
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> getTopicEventList(@PathVariable String topic) {
        final RpcResult rpcResult = topicClient.getTopicEventList(JSONObject.of("topic", topic));
        return createResponse(rpcResult);
    }

    /**
     * 追加事件。同步调用返回处理结果，异步和延迟调用仅返回已提交。
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> appendEvent(
        @PathVariable String topic,
        @RequestParam String eventId,
        @RequestParam(defaultValue = "sync") String mode,
        @RequestParam(defaultValue = "1000") int delayMs
    ) {
        final TopicEventDTO topicEventDTO = new TopicEventDTO();
        topicEventDTO.setTopic(topic);
        topicEventDTO.setEventId(eventId);
        if ("sync".equals(mode)) {
            return createResponse(topicClient.appendEvent(topicEventDTO));
        }
        // 单向调用没有错误回包，在发送前检查参数。
        if (eventId.isBlank()) {
            return ResponseEntity.badRequest().body("事件 ID 不能为空");
        }
        if ("delay".equals(mode) || "delayedOrdered".equals(mode)) {
            if (delayMs < 1 || delayMs > 60000) {
                return ResponseEntity.badRequest().body("延迟时间应为 1 到 60000 毫秒");
            }
            topicEventDTO.setDelay(delayMs);
        }
        switch (mode) {
            case "async" -> topicAsyncClient.appendEvent(topicEventDTO);
            case "ordered" -> topicAsyncClient.appendEventByTopic(topicEventDTO);
            case "delay" -> topicDelayClient.appendEvent(topicEventDTO);
            case "delayedOrdered" -> topicDelayClient.appendEventByTopic(topicEventDTO);
            default -> {
                return ResponseEntity.badRequest().body("不支持的调用类型：" + mode);
            }
        }
        return ResponseEntity.accepted().build();
    }

    private ResponseEntity<?> createResponse(RpcResult rpcResult) {
        if (!rpcResult.isOk()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(rpcResult.getMessage());
        }
        if (rpcResult.getCode() == RCode.DUPLICATE.getCode()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(rpcResult.getMessage());
        }
        if (rpcResult.getCode() != RCode.OK.getCode()) {
            return ResponseEntity.badRequest().body(rpcResult.getMessage());
        }
        return ResponseEntity.ok(rpcResult.getResult());
    }

}
