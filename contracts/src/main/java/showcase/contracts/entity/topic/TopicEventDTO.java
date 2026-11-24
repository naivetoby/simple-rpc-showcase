package showcase.contracts.entity.topic;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import vip.toby.rpc.annotation.RpcDTO;
import vip.toby.rpc.entity.RpcDelayDTO;

@Data
@EqualsAndHashCode(callSuper = true)
@RpcDTO
public class TopicEventDTO extends RpcDelayDTO {

    // 相同话题的事件按此字段路由到同一分区
    @NotBlank(message = "话题不能为空")
    private String topic;

    @NotBlank(message = "事件 ID 不能为空")
    private String eventId;

}
