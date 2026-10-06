package com.carlos.monitor.redis.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * Redis 节点拓扑 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Redis节点拓扑视图对象")
public class RedisNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点地址 host:port")
    private String address;

    @Schema(description = "节点角色：master/slave")
    private String role;

    @Schema(description = "节点状态（connected/ disconnected / link-state）")
    private String status;

    @Schema(description = "负责槽位（cluster，如 0-5460）")
    private String slots;

    @Schema(description = "主节点地址（slave 节点）")
    private String masterAddress;
}
