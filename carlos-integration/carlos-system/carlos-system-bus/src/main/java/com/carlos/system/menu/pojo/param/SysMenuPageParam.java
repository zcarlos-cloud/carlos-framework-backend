package com.carlos.system.menu.pojo.param;

import com.carlos.core.param.ParamPage;
import com.carlos.system.enums.MenuCategory;
import com.carlos.system.enums.MenuStatus;
import com.carlos.system.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 系统菜单 列表查询参数封装
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统菜单列表查询参数")
public class SysMenuPageParam extends ParamPage {

    private static final long serialVersionUID = 2559512989423712792L;

    @Size(max = 200, message = "关键词长度不能超过200")
    @Schema(description = "关键词（菜单名称/路由名）")
    private String keyword;

    @Schema(description = "父级id")
    private Long parentId;

    @Schema(description = "是否在菜单中隐藏")
    private Boolean hideInMenu;

    @Schema(description = "状态")
    private MenuStatus menuStatus;

    @Schema(description = "菜单类别")
    private MenuCategory menuCategory;

    @Schema(description = "客户端类型")
    private MenuType clientType;

    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;

}
