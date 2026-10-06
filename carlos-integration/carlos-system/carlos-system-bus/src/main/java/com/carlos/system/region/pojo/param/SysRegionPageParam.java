package com.carlos.system.region.pojo.param;


import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * <p>
 * 行政区域划分 列表查询参数封装
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:25
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "行政区域划分列表查询参数")
public class SysRegionPageParam extends ParamPage {

    @Size(max = 50, message = "行政区域编码长度不能超过50")
    @Schema(description = "行政区域编码")
    private String regionCode;
    @Size(max = 100, message = "行政区域名称长度不能超过100")
    @Schema(description = "行政区域名称")
    private String regionName;
    @Size(max = 50, message = "上级行政区域编码长度不能超过50")
    @Schema(description = "上级行政区域编码")
    private String parentCode;
    @Size(max = 500, message = "父级code集合长度不能超过500")
    @Schema(description = "父级code集合 ,分隔")
    private String parents;
    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
    @Size(max = 10, message = "区域类型长度不能超过10")
    @Schema(description = "区域类型(0：村下级，1：社区/村，2：街道/乡镇，3：区/县，4：市，5：省/直辖市)")
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
    @Schema(description = "经度")
    private BigDecimal longitude;
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
    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
