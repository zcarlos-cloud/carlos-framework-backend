package com.carlos.auth.passkey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carlos.auth.passkey.pojo.entity.PasskeyCredential;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Passkey 凭据 Mapper 接口
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Mapper
public interface PasskeyCredentialMapper extends BaseMapper<PasskeyCredential> {
}
