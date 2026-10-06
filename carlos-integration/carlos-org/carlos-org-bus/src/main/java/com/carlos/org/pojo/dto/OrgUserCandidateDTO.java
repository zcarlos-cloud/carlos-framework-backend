package com.carlos.org.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 候选用户数据传输对象（选人场景：待办转办、加签、减签、审批人指派等）。
 * </p>
 * <p>
 * 只承载选人清单需要的字段，并带上部门名称；与 {@link OrgUserDTO} 的区别是后者是
 * 用户主体的完整投影（含密码、证件号等），不适合直接外发给调用方。
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgUserCandidateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户主键 */
    private Long userId;

    /** 登录账号 */
    private String code;

    /** 姓名（昵称） */
    private String name;

    /** 归属部门id（取 {@code org_user.main_dept_id}） */
    private Long deptId;

    /** 归属部门名称 */
    private String deptName;

}
