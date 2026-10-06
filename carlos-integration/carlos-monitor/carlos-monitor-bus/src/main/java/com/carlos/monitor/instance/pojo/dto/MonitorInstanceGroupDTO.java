package com.carlos.monitor.instance.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 监控实例分组 数据传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
public class MonitorInstanceGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（雪花ID） */
    private Long id;

    /** 分组名称 */
    private String groupName;

    /** 分组描述 */
    private String description;

    /** 排序号 */
    private Integer orderNum;

    /** 授权角色ID列表（monitor_group_auth） */
    private List<Long> roleIds;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 修改时间 */
    private LocalDateTime updateTime;
}
