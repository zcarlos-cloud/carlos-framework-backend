package com.carlos.system.menu.pojo.vo;

import com.carlos.system.enums.OperationStatus;
import com.carlos.system.enums.OperationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 统一操作表 显示层对象，向页面传输的对象
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysOperationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "操作类型：1=按钮, 2=API")
    private OperationType opType;

    @Schema(description = "所属菜单ID")
    private Long menuId;

    @Schema(description = "操作名称")
    private String opName;

    @Schema(description = "权限码，如 user:create")
    private String permCode;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer orderNum;

    @Schema(description = "API路径")
    private String apiPath;

    @Schema(description = "HTTP方法")
    private String apiMethod;

    @Schema(description = "是否自动扫描：0否 1是")
    private Boolean autoScan;

    @Schema(description = "状态：0禁用 1启用")
    private OperationStatus opStatus;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "修改人")
    private Long updateBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

}
