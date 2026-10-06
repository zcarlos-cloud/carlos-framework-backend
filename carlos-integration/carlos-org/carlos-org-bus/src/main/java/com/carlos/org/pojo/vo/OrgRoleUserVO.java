package com.carlos.org.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 角色关联的用户 VO
 * </p>
 * <p>查看角色用户</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgRoleUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    @Schema(description = "用户id")
    private Long userId;

    /**
     * 账号
     */
    @Schema(description = "账号")
    private String account;

    /**
     * 姓名
     */
    @Schema(description = "姓名")
    private String name;

    /**
     * 手机号码
     */
    @Schema(description = "手机号码")
    private String phone;

    /**
     * 主部门名称
     */
    @Schema(description = "主部门名称")
    private String mainDeptName;

    /**
     * 关联时间
     */
    @Schema(description = "关联时间")
    private LocalDateTime createTime;

}
