package com.carlos.org.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 用户候选对象：用于「选择办理人 / 审批人」这类人员选择场景。
 * </p>
 * <p>
 * 与 {@link OrgUserAO} 的区别是它只保留选人需要的展示字段，并<b>带上部门名称</b>——
 * 调用方（如工作流审批中心）拿到的是一份可以直接渲染的清单，不需要再按部门逐个回查。
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
public class OrgUserCandidateAO implements Serializable {

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
