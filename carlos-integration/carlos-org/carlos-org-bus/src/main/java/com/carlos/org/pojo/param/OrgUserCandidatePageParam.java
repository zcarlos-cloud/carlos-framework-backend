package com.carlos.org.pojo.param;

import com.carlos.core.param.ParamPage;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collection;

/**
 * <p>
 * 候选用户分页查询参数（选人场景）。
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrgUserCandidatePageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    /** 部门id，可选；非空时含该部门的全部子孙部门 */
    private Long deptId;

    /** 限定用户id集合，可选；非空时只在这些用户里挑（空集合语义为「无可选人」） */
    private Collection<Long> includeIds;

    /** 排除用户id集合，可选 */
    private Collection<Long> excludeIds;

}
