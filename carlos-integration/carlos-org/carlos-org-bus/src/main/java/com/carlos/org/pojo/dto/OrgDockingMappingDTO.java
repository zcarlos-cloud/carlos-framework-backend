package com.carlos.org.pojo.dto;


import com.carlos.org.pojo.enums.OrgDockingTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户信息对接关联表 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2025-2-27 15:41:32
 */
@Data
@Accessors(chain = true)
public class OrgDockingMappingDTO {
    /** 主键ID */
    private Long id;
    /** 系统数据id */
    private Long systemId;
    /** 目标系统id */
    private Long targetId;
    /** 目标系统标识 */
    private String targetCode;
    /** 对接类型 */
    private OrgDockingTypeEnum dockingType;
}
