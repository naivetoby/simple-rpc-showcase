package showcase.contracts.client.topic;

import showcase.contracts.entity.topic.TopicEventDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcType;

@RpcClient(name = TopicClient.NAME, type = RpcType.ASYNC, partitionNum = TopicClient.PARTITION_NUM)
public interface TopicAsyncClient {

    @RpcClientMethod("append")
    void appendEvent(TopicEventDTO topicEventDTO);

    @RpcClientMethod(value = "append", partitionKey = "topic")
    void appendEventByTopic(TopicEventDTO topicEventDTO);

}
