package com.carlos.org.api.pojo.param;


import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户部门 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
public class ApiOrgUserDepartmentParam implements Serializable {
    /** 主键 */
    private Long id;
    /** 用户id */
    private Long userId;
    /** 部门id */
    private Long deptId;
    /** 是否为主部门 */
    private Boolean main;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
}
