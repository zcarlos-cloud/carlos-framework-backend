package com.carlos.org.pojo.vo;

import com.carlos.org.pojo.enums.OrgDockingTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 用户信息对接关联表 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2025-2-27 15:41:32
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgDockingMappingVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "系统数据id")
    private Long systemId;
    @Schema(description = "目标系统id")
    private Long targetId;
    @Schema(description = "目标系统标识")
    private String targetCode;
    @Schema(description = "对接类型")
    private OrgDockingTypeEnum dockingType;


}
