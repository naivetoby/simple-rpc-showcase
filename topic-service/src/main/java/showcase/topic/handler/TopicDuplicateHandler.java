package showcase.topic.handler;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;
import vip.toby.rpc.server.RpcServerHandlerInterceptor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TopicDuplicateHandler implements RpcServerHandlerInterceptor {

    // 示例只在单实例内记录每个话题已接收的事件 ID。
    private final Map<String, Set<String>> topicEventIdMap = new ConcurrentHashMap<>();

    @Override
    public boolean duplicateHandle(String method, Object data) {
        if (!(data instanceof JSONObject json)) {
            return false;
        }
        final String topic = json.getString("topic");
        final String eventId = json.getString("eventId");
        // 去重先于 DTO 校验执行，无效请求不占用事件 ID。
        if (topic == null || topic.isBlank() || eventId == null || eventId.isBlank()) {
            return false;
        }
        final Set<String> eventIdSet = topicEventIdMap.computeIfAbsent(topic, key -> ConcurrentHashMap.newKeySet());
        return !eventIdSet.add(eventId);
    }

}
