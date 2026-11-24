package showcase.contracts.client.topic;

import com.alibaba.fastjson2.JSONObject;
import showcase.contracts.entity.topic.TopicEventDTO;
import vip.toby.rpc.annotation.RpcClient;
import vip.toby.rpc.annotation.RpcClientMethod;
import vip.toby.rpc.entity.RpcResult;

@RpcClient(name = TopicClient.NAME)
public interface TopicClient {

    String NAME = "showcase.topic";
    int PARTITION_NUM = 4;

    @RpcClientMethod("history")
    RpcResult getTopicEventList(JSONObject topic);

    @RpcClientMethod("append")
    RpcResult appendEvent(TopicEventDTO topicEventDTO);

}
