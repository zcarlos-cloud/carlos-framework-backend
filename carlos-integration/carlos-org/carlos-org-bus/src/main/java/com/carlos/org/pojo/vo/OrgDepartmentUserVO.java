package com.carlos.org.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 部门人员 VO
 * </p>
 * <p>部门人员列表</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgDepartmentUserVO implements Serializable {

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
     * 性别：0-未知，1-男，2-女
     */
    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;

    /**
     * 手机号码
     */
    @Schema(description = "手机号码")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String email;

    /**
     * 用户状态：0-禁用，1-启用，2-锁定
     */
    @Schema(description = "用户状态：0-禁用，1-启用，2-锁定")
    private Integer userStatus;

    /**
     * 是否主部门
     */
    @Schema(description = "是否主部门")
    private Boolean mainDept;

    /**
     * 入职时间
     */
    @Schema(description = "入职时间")
    private LocalDateTime joinTime;

}
