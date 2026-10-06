package com.carlos.system.sequence.convert;

import com.carlos.system.sequence.pojo.dto.SysSequenceRuleDTO;
import com.carlos.system.sequence.pojo.entity.SysSequenceRule;
import com.carlos.system.sequence.pojo.enums.SequenceResetCycleEnum;
import com.carlos.system.sequence.pojo.param.SysSequenceRuleCreateParam;
import com.carlos.system.sequence.pojo.param.SysSequenceRuleUpdateParam;
import com.carlos.system.sequence.pojo.vo.SysSequenceRuleVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 业务编码/序列号规则 对象转换器
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Mapper
public interface SysSequenceRuleConvert {

    SysSequenceRuleConvert INSTANCE = Mappers.getMapper(SysSequenceRuleConvert.class);

    /**
     * 新增参数转 DTO（Integer 编码转枚举；计数水位由生成动作维护，此处忽略）
     *
     * @param param 新增参数
     * @return 规则数据
     */
    @Mappings({
        @Mapping(target = "resetCycle", expression = "java(parseResetCycle(param.getResetCycle()))"),
        @Mapping(target = "currentValue", ignore = true),
        @Mapping(target = "currentPeriod", ignore = true),
        @Mapping(target = "tenantId", ignore = true)
    })
    SysSequenceRuleDTO toDTO(SysSequenceRuleCreateParam param);

    /**
     * 更新参数转 DTO（规则编码不可改；计数水位由生成动作维护，此处忽略）
     *
     * @param param 更新参数
     * @return 规则数据
     */
    @Mappings({
        @Mapping(target = "ruleCode", ignore = true),
        @Mapping(target = "resetCycle", expression = "java(parseResetCycle(param.getResetCycle()))"),
        @Mapping(target = "currentValue", ignore = true),
        @Mapping(target = "currentPeriod", ignore = true),
        @Mapping(target = "tenantId", ignore = true)
    })
    SysSequenceRuleDTO toDTO(SysSequenceRuleUpdateParam param);

    /**
     * 实体转 DTO
     *
     * @param entity 实体
     * @return 规则数据
     */
    SysSequenceRuleDTO toDTO(SysSequenceRule entity);

    /**
     * DTO 转实体
     *
     * @param dto 规则数据
     * @return 实体
     */
    SysSequenceRule toDO(SysSequenceRuleDTO dto);

    /**
     * DTO 转 VO（枚举拆为编码 + 名称两字段）
     *
     * @param dto 规则数据
     * @return 视图对象
     */
    @Mappings({
        @Mapping(target = "resetCycle", expression = "java(toResetCycleCode(dto.getResetCycle()))"),
        @Mapping(target = "resetCycleName", expression = "java(toResetCycleName(dto.getResetCycle()))")
    })
    SysSequenceRuleVO toVO(SysSequenceRuleDTO dto);

    /**
     * 实体转 VO（枚举拆为编码 + 名称两字段）
     *
     * @param entity 实体
     * @return 视图对象
     */
    @Mappings({
        @Mapping(target = "resetCycle", expression = "java(toResetCycleCode(entity.getResetCycle()))"),
        @Mapping(target = "resetCycleName", expression = "java(toResetCycleName(entity.getResetCycle()))")
    })
    SysSequenceRuleVO toVO(SysSequenceRule entity);

    /**
     * 实体列表转 VO 列表
     *
     * @param dos 实体列表
     * @return 视图对象列表
     */
    List<SysSequenceRuleVO> toVO(List<SysSequenceRule> dos);

    /**
     * 编码转枚举
     */
    default SequenceResetCycleEnum parseResetCycle(Integer code) {
        return SequenceResetCycleEnum.of(code);
    }

    /**
     * 枚举转编码
     */
    default Integer toResetCycleCode(SequenceResetCycleEnum resetCycle) {
        return resetCycle == null ? null : resetCycle.getCode();
    }

    /**
     * 枚举转名称
     */
    default String toResetCycleName(SequenceResetCycleEnum resetCycle) {
        return resetCycle == null ? null : resetCycle.getDesc();
    }

}
