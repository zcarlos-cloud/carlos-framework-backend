package com.carlos.system.pojo.ao;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 行政区域划分 数据传输对象，service和manager向外传输对象
 *
 * @author carlos
 * @date 2022-11-8 19:30:24
 */
@Data
@Accessors(chain = true)
public class SysRegionAO {

    /**
     * 自增ID
     */
    private Long id;
    /**
     * 行政区域编码
     */
    private String regionCode;
    /**
     * 行政区域名称
     */
    private String regionName;
    /**
     * 上级行政区域编码
     */
    private String parentCode;
    /**
     * 父级code集合 ,分隔
     */
    private String parents;
    /**
     * 备注
     */
    private String remark;
    /**
     * 区域类型(0：村下级，1：社区/村，2：街道/乡镇，3：区/县，4：市，5：省/直辖市)
     */
    private String regionType;
    /**
     * 区域面积(KM2)
     */
    private BigDecimal regionArea;
    /**
     * 区域人口
     */
    private Long regionPeopleNumber;
    /**
     * 区域级别
     */
    private Integer regionLevel;
    /**
     * 区域负责人
     */
    private String regionLeader;
    /**
     * 扩展字段值（网格：1、综合网格；2、专属网格；3、镇街网格；4村社区网格；5、基础网格） （社区：1、村社区；2、镇街社区；）
     */
    private Long extend;
    /**
     * 经度
     */
    private BigDecimal longitude;
    /**
     * 纬度
     */
    private BigDecimal latitude;
    /**
     * 行政区左边界经度
     */
    private BigDecimal boundLeft;
    /**
     * 行政区右边界经度
     */
    private BigDecimal boundRight;
    /**
     * 行政区下边界纬度
     */
    private BigDecimal boundBottom;
    /**
     * 行政区上边界纬度
     */
    private BigDecimal boundTop;
    /**
     * gis标绘信息，大于0整数代表已标绘
     */
    private Long gisOid;
    /**
     * 创建人
     */
    private Long createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    private Long updateBy;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 子级数目
     */
    private Long subNum;

    /**
     * 子级
     */
    private List<SysRegionAO> children;
}
