package com.carlos.system.menu.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 系统菜单 显示层对象，向页面传输的对象
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysMenuRecursionVO extends SysMenuVO {

    @Schema(description = "子菜单信息")
    List<SysMenuRecursionVO> children;

}
