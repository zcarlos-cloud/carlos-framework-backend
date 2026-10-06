package com.carlos.auth.mfa.convert;

import com.carlos.auth.mfa.pojo.dto.MfaSetupDTO;
import com.carlos.auth.mfa.pojo.entity.MfaRecoveryCode;
import com.carlos.auth.mfa.pojo.vo.MfaSetupVO;
import com.carlos.auth.mfa.pojo.vo.MfaStatusVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * MFA 对象转换器
 *
 * <p>使用 MapStruct 实现 Param/VO/DTO/Entity 之间的转换。</p>
 *
 * <p><strong>转换规则：</strong></p>
 * <ul>
 *   <li>Param → Entity: 接收前端参数，转换为数据库实体</li>
 *   <li>DTO → VO: 服务层数据转换为视图对象返回给前端</li>
 *   <li>Entity → VO: 数据库实体转换为视图对象</li>
 * </ul>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see com.carlos.auth.mfa.pojo.dto.MfaSetupDTO
 * @see com.carlos.auth.mfa.pojo.vo.MfaSetupVO
 * @see com.carlos.auth.mfa.pojo.entity.MfaRecoveryCode
 */
@Mapper
public interface MfaConvert {

    MfaConvert INSTANCE = Mappers.getMapper(MfaConvert.class);

    /**
     * DTO 转 VO
     *
     * @param dto MFA设置DTO
     * @return MFA设置VO
     */
    MfaSetupVO dtoToVo(MfaSetupDTO dto);

    /**
     * 批量 DTO 转 VO
     *
     * @param dtoList MFA设置DTO列表
     * @return MFA设置VO列表
     */
    List<MfaSetupVO> dtoListToVoList(List<MfaSetupDTO> dtoList);

    /**
     * Entity 转 VO
     *
     * @param entity 恢复码实体
     * @return 恢复码字符串
     */
    default String entityToCode(MfaRecoveryCode entity) {
        if (entity == null) {
            return null;
        }
        return entity.getCode();
    }

    /**
     * 构建 MFA 状态 VO
     *
     * @param enabled 是否启用
     * @return MFA状态VO
     */
    default MfaStatusVO buildStatusVO(boolean enabled) {
        return MfaStatusVO.builder()
            .enabled(enabled)
            .mfaType("TOTP")
            .build();
    }
}
