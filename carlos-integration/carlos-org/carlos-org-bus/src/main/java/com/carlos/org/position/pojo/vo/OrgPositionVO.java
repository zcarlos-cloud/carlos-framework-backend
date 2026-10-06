package com.carlos.org.position.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 岗位表 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgPositionVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "岗位编码，租户内唯一，如：JAVA_DEV_001")
    private String positionCode;
    @Schema(description = "岗位名称，如：Java开发工程师")
    private String positionName;
    @Schema(description = "岗位简称，如：Java开发")
    private String positionShort;
    @Schema(description = "所属职系ID")
    private Long categoryId;
    @Schema(description = "默认职级ID（新人入职默认职级）")
    private Long defaultLevelId;
    @Schema(description = "职级范围，如：T1-T5，支持的范围")
    private String levelRange;
    @Schema(description = "岗位类型：1标准岗（有编制），2虚拟岗（项目制），3兼职岗")
    private Integer positionType;
    @Schema(description = "岗位性质：1全职，2兼职，3实习，4外包")
    private Integer positionNature;
    @Schema(description = "所属部门ID（标准岗必填，虚拟岗可为空）")
    private Long departmentId;
    @Schema(description = "上级岗位ID，用于汇报线，0表示无上级")
    private Long parentId;
    @Schema(description = "编制人数（标准岗），0表示不限")
    private Integer headcount;
    @Schema(description = "当前在岗人数（冗余，实时计算）")
    private Integer currentCount;
    @Schema(description = "空缺人数（冗余，实时计算）")
    private Integer vacancyCount;
    @Schema(description = "岗位职责（富文本）")
    private String duty;
    @Schema(description = "任职要求（富文本）")
    private String requirement;
    @Schema(description = "任职资格（JSON格式，支持扩展）")
    private String qualification;
    @Schema(description = "绩效考核指标（JSON格式）")
    private String kpiIndicator;
    @Schema(description = "成本中心编码，对接财务系统")
    private String costCenter;
    @Schema(description = "工作地点，如：北京、上海、远程")
    private String location;
    @Schema(description = "薪等，如：P5、P6")
    private String salaryGrade;
    @Schema(description = "状态：0禁用，1启用，2冻结（暂停招聘）")
    private Integer positionStatus;
    @Schema(description = "租户ID")
    private Long tenantId;
    @Schema(description = "创建者")
    private Long createBy;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "修改者")
    private Long updateBy;
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

}
