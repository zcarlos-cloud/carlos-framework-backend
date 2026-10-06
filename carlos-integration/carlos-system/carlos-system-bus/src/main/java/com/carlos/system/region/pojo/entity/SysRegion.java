package com.carlos.system.region.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 行政区域划分 数据源对象
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:24
 */
@Data
@Accessors(chain = true)
@TableName("sys_region")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysRegion implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 自增ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 行政区域编码
     */
    @TableField(value = "region_code")
    private String regionCode;
    /**
     * 行政区域名称
     */
    @TableField(value = "region_name")
    private String regionName;
    /**
     * 上级行政区域编码
     */
    @TableField(value = "parent_code")
    private String parentCode;
    /**
     * 父级code集合 ,分隔
     */
    @TableField(value = "parents")
    private String parents;
    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
    /**
     * 区域类型(0：村下级，1：社区/村，2：街道/乡镇，3：区/县，4：市，5：省/直辖市)
     */
    @TableField(value = "region_type")
    private String regionType;
    /**
     * 区域面积(KM2)
     */
    @TableField(value = "region_area")
    private BigDecimal regionArea;
    /**
     * 区域人口
     */
    @TableField(value = "region_people_number")
    private Long regionPeopleNumber;
    /**
     * 区域级别
     */
    @TableField(value = "region_level")
    private Integer regionLevel;
    /**
     * 区域负责人
     */
    @TableField(value = "region_leader")
    private String regionLeader;
    /**
     * 扩展字段值（网格：1、综合网格；2、专属网格；3、镇街网格；4村社区网格；5、基础网格） （社区：1、村社区；2、镇街社区；）
     */
    @TableField(value = "extend")
    private Long extend;
    /**
     * 经度
     */
    @TableField(value = "longitude")
    private BigDecimal longitude;
    /**
     * 纬度
     */
    @TableField(value = "latitude")
    private BigDecimal latitude;
    /**
     * 行政区左边界经度
     */
    @TableField(value = "bound_left")
    private BigDecimal boundLeft;
    /**
     * 行政区右边界经度
     */
    @TableField(value = "bound_right")
    private BigDecimal boundRight;
    /**
     * 行政区下边界纬度
     */
    @TableField(value = "bound_bottom")
    private BigDecimal boundBottom;
    /**
     * 行政区上边界纬度
     */
    @TableField(value = "bound_top")
    private BigDecimal boundTop;
    /**
     * gis标绘信息，大于0整数代表已标绘
     */
    @TableField(value = "gis_oid")
    private Long gisOid;
    /**
     * 删除标记(0.未删除  1.已删除)
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 排序
     */
    @TableField(value = "order_num")
    private int orderNum;

}
