package com.carlos.org.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户部门 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
public class OrgUserDepartmentDTO {
    /** 主键 */
    private Long id;
    /** 用户id */
    private Long userId;
    /** 部门id */
    private Long deptId;
    /** 是否为主部门 */
    private Boolean mainDept;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
}
