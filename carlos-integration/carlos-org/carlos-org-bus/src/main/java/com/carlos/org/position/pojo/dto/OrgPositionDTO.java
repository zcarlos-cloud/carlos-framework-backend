package com.carlos.org.position.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 岗位表 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
public class OrgPositionDTO {
    /** 主键 */
    private Long id;
    /** 岗位编码，租户内唯一，如：JAVA_DEV_001 */
    private String positionCode;
    /** 岗位名称，如：Java开发工程师 */
    private String positionName;
    /** 岗位简称，如：Java开发 */
    private String positionShort;
    /** 所属职系ID */
    private Long categoryId;
    /** 默认职级ID（新人入职默认职级） */
    private Long defaultLevelId;
    /** 职级范围，如：T1-T5，支持的范围 */
    private String levelRange;
    /** 岗位类型：1标准岗（有编制），2虚拟岗（项目制），3兼职岗 */
    private Integer positionType;
    /** 岗位性质：1全职，2兼职，3实习，4外包 */
    private Integer positionNature;
    /** 所属部门ID（标准岗必填，虚拟岗可为空） */
    private Long departmentId;
    /** 上级岗位ID，用于汇报线，0表示无上级 */
    private Long parentId;
    /** 编制人数（标准岗），0表示不限 */
    private Integer headcount;
    /** 当前在岗人数（冗余，实时计算） */
    private Integer currentCount;
    /** 空缺人数（冗余，实时计算） */
    private Integer vacancyCount;
    /** 岗位职责（富文本） */
    private String duty;
    /** 任职要求（富文本） */
    private String requirement;
    /** 任职资格（JSON格式，支持扩展） */
    private String qualification;
    /** 绩效考核指标（JSON格式） */
    private String kpiIndicator;
    /** 成本中心编码，对接财务系统 */
    private String costCenter;
    /** 工作地点，如：北京、上海、远程 */
    private String location;
    /** 薪等，如：P5、P6 */
    private String salaryGrade;
    /** 状态：0禁用，1启用，2冻结（暂停招聘） */
    private Integer positionStatus;
    /** 乐观锁版本号 */
    private Integer version;
    /** 租户ID */
    private Long tenantId;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 修改者 */
    private Long updateBy;
    /** 修改时间 */
    private LocalDateTime updateTime;
}
