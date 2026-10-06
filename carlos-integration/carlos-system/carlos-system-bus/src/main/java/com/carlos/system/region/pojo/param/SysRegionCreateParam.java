package com.carlos.system.region.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;


/**
 * <p>
 * 行政区域划分 新增参数封装
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:24
 */
@Data
@Accessors(chain = true)
@Schema(description = "行政区域划分新增参数")
public class SysRegionCreateParam {

    @NotBlank(message = "行政区域编码不能为空")
    @Size(max = 50, message = "行政区域编码长度不能超过50")
    @Schema(description = "行政区域编码")
    private String regionCode;
    @NotBlank(message = "行政区域名称不能为空")
    @Size(max = 100, message = "行政区域名称长度不能超过100")
    @Schema(description = "行政区域名称")
    private String regionName;
    /**
     * 上级行政区域编码
     *
     * <p>
     * <b>可空</b>：留空表示新建**顶级区域**。库中 `parent_code` 也可空，且后端
     * 全链路（{@code RegionConstant.TOP_REGION_PARENT_CODE}、逐级加载、按 code 导入、
     * 区域树取根）都以字面量 "0" 认根 —— 故这里不再强制必填，由
     * {@code SysRegionService#addSysRegion} 把空值归一化为 "0"。
     * </p>
     *
     * <p>
     * 此前这里带 {@code @NotBlank}，导致顶级区域**在接口层就被拒**，前端无论怎么改都建不出来。
     * </p>
     */
    @Size(max = 50, message = "上级行政区域编码长度不能超过50")
    @Schema(description = "上级行政区域编码，留空表示顶级区域")
    private String parentCode;
    @Size(max = 500, message = "父级code集合长度不能超过500")
    @Schema(description = "父级code集合 ,分隔")
    private String parents;
    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
    /**
     * 区域类型(0：村下级，1：社区/村，2：街道/乡镇，3：区/县，4：市，5：省/直辖市)
     *
     * <p>
     * 只读派生字段：{@code SysRegionService#setRegionType} 在新增与修改时都会按上级重新推导，
     * 传入值一律被覆盖，故不接受调用方指定。
     * </p>
     */
    @Size(max = 10, message = "区域类型长度不能超过10")
    @Schema(description = "区域类型(0：村下级，1：社区/村，2：街道/乡镇，3：区/县，4：市，5：省/直辖市)，由上级自动推导，传入无效")
    private String regionType;
    @Schema(description = "区域面积(KM2)")
    private BigDecimal regionArea;
    @Schema(description = "区域人口")
    private Long regionPeopleNumber;
    @Schema(description = "区域级别")
    private Integer regionLevel;
    @Size(max = 50, message = "区域负责人长度不能超过50")
    @Schema(description = "区域负责人")
    private String regionLeader;
    @Schema(description = "扩展字段值（网格：1、综合网格；2、专属网格；3、镇街网格；4村社区网格；5、基础网格）（社区：1、村社区；2、镇街社区；）")
    private Long extend;
    @DecimalMin(value = "-180", message = "经度不能小于-180")
    @DecimalMax(value = "180", message = "经度不能大于180")
    @Schema(description = "经度")
    private BigDecimal longitude;
    @DecimalMin(value = "-90", message = "纬度不能小于-90")
    @DecimalMax(value = "90", message = "纬度不能大于90")
    @Schema(description = "纬度")
    private BigDecimal latitude;
    @Schema(description = "行政区左边界经度")
    private BigDecimal boundLeft;
    @Schema(description = "行政区右边界经度")
    private BigDecimal boundRight;
    @Schema(description = "行政区下边界纬度")
    private BigDecimal boundBottom;
    @Schema(description = "行政区上边界纬度")
    private BigDecimal boundTop;
    @Schema(description = "gis标绘信息，大于0整数代表已标绘")
    private Long gisOid;
    @Schema(description = "排序")
    private int orderNum;
}
