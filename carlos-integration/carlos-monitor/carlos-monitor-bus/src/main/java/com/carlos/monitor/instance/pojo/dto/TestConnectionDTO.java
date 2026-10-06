package com.carlos.monitor.instance.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 连接测试结果 数据传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
public class TestConnectionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否连接成功 */
    private Boolean success;

    /** 服务端版本（redis_version / 数据库版本） */
    private String version;

    /** 角色（Redis：master/slave；DB 为空） */
    private String role;

    /** 连接耗时（毫秒） */
    private Long elapsedMs;

    /** 节点明细（cluster/sentinel 模式返回各节点） */
    private List<NodeInfo> nodes;

    /** 失败原因（已脱敏，不含密码等敏感信息） */
    private String errorMsg;

    /**
     * <p>
     * 节点信息
     * </p>
     */
    @Data
    @Accessors(chain = true)
    public static class NodeInfo implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 节点地址 host:port */
        private String address;

        /** 节点角色：master/slave/replica */
        private String role;

        /** 节点状态（CLUSTER NODES flags，或连接状态） */
        private String status;
    }
}
