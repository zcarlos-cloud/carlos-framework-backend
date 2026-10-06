package com.carlos.org.position.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 岗位表 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Data
@Accessors(chain = true)
@Schema(description = "岗位表修改参数")
public class OrgPositionUpdateParam {
    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
    // 与前端 src/constants/reg.ts 的 REG_BIZ_CODE 保持一致：字母开头，仅字母、数字、下划线、点、中划线，长度2-64
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "岗位编码须以字母开头，仅允许字母、数字、下划线、点、中划线，长度2-64")
    @Size(max = 50, message = "岗位编码长度不能超过50")
    @Schema(description = "岗位编码，租户内唯一，如：JAVA_DEV_001")
    private String positionCode;
    @Size(max = 50, message = "岗位名称长度不能超过50")
    @Schema(description = "岗位名称，如：Java开发工程师")
    private String positionName;
    @Size(max = 20, message = "岗位简称长度不能超过20")
    @Schema(description = "岗位简称，如：Java开发")
    private String positionShort;
    @Schema(description = "所属职系ID")
    private Long categoryId;
    @Schema(description = "默认职级ID（新人入职默认职级）")
    private Long defaultLevelId;
    @Size(max = 100, message = "职级范围长度不能超过100")
    @Schema(description = "职级范围，如：T1-T5，支持的范围")
    private String levelRange;
    @Min(value = 1, message = "岗位类型取值 1-3")
    @Max(value = 3, message = "岗位类型取值 1-3")
    @Schema(description = "岗位类型：1标准岗（有编制），2虚拟岗（项目制），3兼职岗")
    private Integer positionType;
    @Min(value = 1, message = "岗位性质取值 1-4")
    @Max(value = 4, message = "岗位性质取值 1-4")
    @Schema(description = "岗位性质：1全职，2兼职，3实习，4外包")
    private Integer positionNature;
    @Schema(description = "所属部门ID（标准岗必填，虚拟岗可为空）")
    private Long departmentId;
    @Schema(description = "上级岗位ID，用于汇报线，0表示无上级")
    private Long parentId;
    @Min(value = 0, message = "编制人数不能小于0")
    @Schema(description = "编制人数（标准岗），0表示不限")
    private Integer headcount;
    @Schema(description = "当前在岗人数（冗余，实时计算）")
    private Integer currentCount;
    @Schema(description = "空缺人数（冗余，实时计算）")
    private Integer vacancyCount;
    @Size(max = 65535, message = "岗位职责长度不能超过65535")
    @Schema(description = "岗位职责（富文本）")
    private String duty;
    @Size(max = 65535, message = "任职要求长度不能超过65535")
    @Schema(description = "任职要求（富文本）")
    private String requirement;
    @Size(max = 65535, message = "任职资格长度不能超过65535")
    @Schema(description = "任职资格（JSON格式，支持扩展）")
    private String qualification;
    @Size(max = 65535, message = "绩效考核指标长度不能超过65535")
    @Schema(description = "绩效考核指标（JSON格式）")
    private String kpiIndicator;
    @Size(max = 50, message = "成本中心编码长度不能超过50")
    @Schema(description = "成本中心编码，对接财务系统")
    private String costCenter;
    @Size(max = 100, message = "工作地点长度不能超过100")
    @Schema(description = "工作地点，如：北京、上海、远程")
    private String location;
    @Size(max = 20, message = "薪等长度不能超过20")
    @Schema(description = "薪等，如：P5、P6")
    private String salaryGrade;
    @Min(value = 0, message = "状态取值 0-2")
    @Max(value = 2, message = "状态取值 0-2")
    @Schema(description = "状态：0禁用，1启用，2冻结（暂停招聘）")
    private Integer positionStatus;
    @Schema(description = "租户ID")
    private Long tenantId;
}
