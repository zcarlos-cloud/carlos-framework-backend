package com.carlos.system.region.pojo.excel;


import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 部门 导入导出数据对象
 * </p>
 */
@Data
public class RegionExcel implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;
    /**
     * 行政区域编码
     */
    @ExcelProperty(value = "行政区域编码")
    private String regionCode;
    /**
     * 行政区域名称
     */
    @ExcelProperty(value = "行政区域名称")
    private String regionName;
    /**
     * 上级行政区域编码
     */
    @ExcelProperty(value = "上级行政区域编码")
    private String parentCode;
    /**
     * 父级code集合 ,分隔
     */
    @ExcelProperty(value = "父级code集合 ,分隔")
    private String parents;
    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;
    /**
     * 区域类型(0：村下级，1：社区/村，2：街道/乡镇，3：区/县，4：市，5：省/直辖市)
     */
    @ExcelProperty(value = "区域类型")
    private String regionType;
    /**
     * 区域面积(KM2)
     */
    @ExcelProperty(value = "区域面积(KM2)")
    private BigDecimal regionArea;
    /**
     * 区域人口
     */
    @ExcelProperty(value = "区域人口")
    private Long regionPeopleNumber;
    /**
     * 区域级别
     */
    @ExcelProperty(value = "区域级别")
    private Integer regionLevel;
    /**
     * 区域负责人
     */
    @ExcelProperty(value = "区域负责人")
    private String regionLeader;
    /**
     * 扩展字段值（网格：1、综合网格；2、专属网格；3、镇街网格；4村社区网格；5、基础网格） （社区：1、村社区；2、镇街社区；）
     */
    @ExcelProperty(value = "扩展字段值")
    private Long extend;
    /**
     * 经度
     */
    @ExcelProperty(value = "经度")
    private BigDecimal longitude;
    /**
     * 纬度
     */
    @ExcelProperty(value = "纬度")
    private BigDecimal latitude;
    /**
     * 行政区左边界经度
     */
    @ExcelProperty(value = "行政区左边界经度")
    private BigDecimal boundLeft;
    /**
     * 行政区右边界经度
     */
    @ExcelProperty(value = "行政区右边界经度")
    private BigDecimal boundRight;
    /**
     * 行政区下边界纬度
     */
    @ExcelProperty(value = "行政区下边界纬度")
    private BigDecimal boundBottom;
    /**
     * 行政区上边界纬度
     */
    @ExcelProperty(value = "行政区上边界纬度")
    private BigDecimal boundTop;
    /**
     * gis标绘信息，大于0整数代表已标绘
     */
    @ExcelProperty(value = "gis标绘信息")
    private Long gisOid;


}
