package com.carlos.org.position.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 岗位类别表（职系） API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
public class ApiOrgPositionCategoryParam implements Serializable {
    /** 主键 */
    private Long id;
    /** 类别编码，如：M、T、P、S */
    private String categoryCode;
    /** 类别名称，如：管理系、技术系、专业系、销售系 */
    private String categoryName;
    /** 类别类型：1管理通道，2专业通道，3双通道 */
    private Integer categoryType;
    /** 类别描述 */
    private String description;
    /** 排序 */
    private Integer orderNum;
    /** 状态：0禁用，1启用 */
    private Integer positionCategoryStatus;
    /** 乐观锁版本号 */
    private Integer version;
    /** 租户ID */
    private Long tenantId;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 修改者 */
    private Long updateBy;
    /** 修改时间 */
    private LocalDateTime updateTime;
}
