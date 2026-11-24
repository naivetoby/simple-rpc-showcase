package showcase.topic.server;

import com.alibaba.fastjson2.JSONObject;
import jakarta.validation.Valid;
import showcase.contracts.client.topic.TopicClient;
import showcase.contracts.entity.topic.TopicEventDTO;
import vip.toby.rpc.annotation.RpcServer;
import vip.toby.rpc.annotation.RpcServerMethod;
import vip.toby.rpc.entity.R;
import vip.toby.rpc.entity.RpcType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RpcServer(
    name = TopicClient.NAME,
    type = {RpcType.SYNC, RpcType.ASYNC, RpcType.DELAY},
    partitionNum = TopicClient.PARTITION_NUM
)
public class TopicServer {

    private final Map<String, List<String>> topicEventMap = new ConcurrentHashMap<>();

    @RpcServerMethod("append")
    public R appendEvent(@Valid TopicEventDTO topicEventDTO) {
        final List<String> eventList = topicEventMap.computeIfAbsent(
            topicEventDTO.getTopic(),
            topic -> new CopyOnWriteArrayList<>()
        );
        eventList.add(topicEventDTO.getEventId());
        return R.ok();
    }

    @RpcServerMethod(value = "history", allowDuplicate = true)
    public R getTopicEventList(JSONObject query) {
        final List<String> eventList = topicEventMap.getOrDefault(query.getString("topic"), List.of());
        return R.okResult(List.copyOf(eventList));
    }

}
