package com.carlos.system.region.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 行政区域划分 显示层对象，向页面传输的对象
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:25
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysRegionVO implements Serializable {

    private static final long serialVersionUID = 1L;
    @Schema(description = "自增ID")
    private Long id;
    @Schema(description = "行政区域编码")
    private String regionCode;
    @Schema(description = "行政区域名称")
    private String regionName;
    @Schema(description = "上级行政区域编码")
    private String parentCode;
    /**
     * 上级行政区域名称
     *
     * <p>
     * 非表字段，由 {@code SysRegionManagerImpl#getPage} 按当前页的 parentCode 批量回填。
     * 列表里只有编码时使用者没法判断这一行挂在哪，故一并下发名称；
     * 顶级区域的 parentCode 是哨兵 "0"，此处为 null。
     * </p>
     */
    @Schema(description = "上级行政区域名称（顶级区域为空）")
    private String parentName;
    @Schema(description = "父级code集合 ,分隔")
    private String parents;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "区域类型(0：村下级，1：社区/村，2：街道/乡镇，3：区/县，4：市，5：省/直辖市)")
    private String regionType;
    @Schema(description = "区域面积(KM2)")
    private BigDecimal regionArea;
    @Schema(description = "区域人口")
    private Long regionPeopleNumber;
    @Schema(description = "区域级别")
    private Integer regionLevel;
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
    @Schema(description = "排序")
    private int orderNum;
    // @UserIdField(type = UserIdField.SerializerType.REALNAME)
    // @Schema(description = "创建人")
    // private Long createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新人")
    private Long updateBy;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "子级数目")
    private Long subNum;

    @Schema(description = "子区域信息")
    List<SysRegionVO> children = new ArrayList<>();

}
