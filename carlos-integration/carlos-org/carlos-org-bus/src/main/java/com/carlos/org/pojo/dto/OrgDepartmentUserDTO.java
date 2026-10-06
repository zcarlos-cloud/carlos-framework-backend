package com.carlos.org.pojo.dto;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * <p>
 * 部门人员 DTO
 * </p>
 * <p>部门人员列表</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgDepartmentUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 账号
     */
    private String account;

    /**
     * 姓名
     */
    private String name;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户状态：0-禁用，1-启用，2-锁定
     */
    private Integer userStatus;

    /**
     * 是否主部门
     */
    private Boolean mainDept;

    /**
     * 入职时间
     */
    private LocalDateTime joinTime;

}
