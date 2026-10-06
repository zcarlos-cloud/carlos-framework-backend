package com.carlos.auth.mfa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.auth.mfa.pojo.entity.MfaRecoveryCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * MFA备用恢复码 Mapper 接口
 * </p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Mapper
public interface MfaRecoveryCodeMapper extends BaseMapper<MfaRecoveryCode> {
}
