package com.carlos.system.news.convert;

import com.carlos.system.news.pojo.dto.SysNewsDTO;
import com.carlos.system.news.pojo.entity.SysNews;
import com.carlos.system.news.pojo.enums.SysNewsScopeEnum;
import com.carlos.system.news.pojo.enums.SysNewsTypeEnum;
import com.carlos.system.news.pojo.param.SysNewsCreateParam;
import com.carlos.system.news.pojo.param.SysNewsUpdateParam;
import com.carlos.system.news.pojo.vo.SysNewsVO;
import com.carlos.system.pojo.ao.SysNewsAO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * <p>
 * 公告/站内通知 对象转换器
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Mapper
public interface SysNewsConvert {

    SysNewsConvert INSTANCE = Mappers.getMapper(SysNewsConvert.class);

    /**
     * 新增参数转 DTO（Integer 编码转枚举；scopeRoleIds 列表由 Service 序列化，此处忽略）
     *
     * @param param 新增参数
     * @return 公告数据
     */
    @Mappings({
        @Mapping(target = "newsType", expression = "java(parseType(param.getNewsType()))"),
        @Mapping(target = "publishScope", expression = "java(parseScope(param.getPublishScope()))"),
        @Mapping(target = "scopeRoleIds", ignore = true),
        @Mapping(target = "status", ignore = true),
        @Mapping(target = "publishTime", ignore = true),
        @Mapping(target = "tenantId", ignore = true)
    })
    SysNewsDTO toDTO(SysNewsCreateParam param);

    /**
     * 更新参数转 DTO（Integer 编码转枚举；状态/发布时间由发布动作管理，此处忽略）
     *
     * @param param 更新参数
     * @return 公告数据
     */
    @Mappings({
        @Mapping(target = "newsType", expression = "java(parseType(param.getNewsType()))"),
        @Mapping(target = "publishScope", expression = "java(parseScope(param.getPublishScope()))"),
        @Mapping(target = "scopeRoleIds", ignore = true),
        @Mapping(target = "status", ignore = true),
        @Mapping(target = "publishTime", ignore = true),
        @Mapping(target = "tenantId", ignore = true)
    })
    SysNewsDTO toDTO(SysNewsUpdateParam param);

    /**
     * 解析公告类型编码
     *
     * @param code 类型编码
     * @return 枚举，未知编码返回 null
     */
    default SysNewsTypeEnum parseType(Integer code) {
        if (code == null) {
            return null;
        }
        for (SysNewsTypeEnum value : SysNewsTypeEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 解析发布范围编码
     *
     * @param code 范围编码
     * @return 枚举，未知编码返回 null
     */
    default SysNewsScopeEnum parseScope(Integer code) {
        if (code == null) {
            return null;
        }
        for (SysNewsScopeEnum value : SysNewsScopeEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * DTO 转实体
     *
     * @param dto 公告数据
     * @return 实体
     */
    SysNews toDO(SysNewsDTO dto);

    /**
     * 实体转 DTO
     *
     * @param entity 实体
     * @return 公告数据
     */
    SysNewsDTO toDTO(SysNews entity);

    /**
     * DTO 转 VO（枚举拆为编码 + 名称两个字段）
     *
     * @param dto 公告数据
     * @return 视图对象
     */
    @Mappings({
        @Mapping(target = "newsType", expression = "java(dto.getNewsType() == null ? null : dto.getNewsType().getCode())"),
        @Mapping(target = "newsTypeName", expression = "java(dto.getNewsType() == null ? null : dto.getNewsType().getDesc())"),
        @Mapping(target = "publishScope", expression = "java(dto.getPublishScope() == null ? null : dto.getPublishScope().getCode())"),
        @Mapping(target = "publishScopeName", expression = "java(dto.getPublishScope() == null ? null : dto.getPublishScope().getDesc())"),
        @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getCode())"),
        @Mapping(target = "statusName", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getDesc())"),
        @Mapping(target = "isRead", ignore = true)
    })
    SysNewsVO toVO(SysNewsDTO dto);

    /**
     * DTO 转 AO（Feign 调用方）
     *
     * @param dto 公告数据
     * @return API 对象
     */
    @Mappings({
        @Mapping(target = "newsType", expression = "java(dto.getNewsType() == null ? null : dto.getNewsType().getCode())"),
        @Mapping(target = "publishScope", expression = "java(dto.getPublishScope() == null ? null : dto.getPublishScope().getCode())"),
        @Mapping(target = "status", expression = "java(dto.getStatus() == null ? null : dto.getStatus().getCode())")
    })
    SysNewsAO toAO(SysNewsDTO dto);

}
