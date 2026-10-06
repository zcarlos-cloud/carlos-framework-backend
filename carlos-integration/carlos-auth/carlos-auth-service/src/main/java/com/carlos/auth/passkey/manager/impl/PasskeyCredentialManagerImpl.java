package com.carlos.auth.passkey.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.carlos.auth.passkey.manager.PasskeyCredentialManager;
import com.carlos.auth.passkey.mapper.PasskeyCredentialMapper;
import com.carlos.auth.passkey.pojo.entity.PasskeyCredential;
import com.carlos.datasource.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * Passkey 凭据查询封装实现
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@Service
public class PasskeyCredentialManagerImpl extends BaseServiceImpl<PasskeyCredentialMapper, PasskeyCredential>
        implements PasskeyCredentialManager {

    @Override
    public boolean saveCredential(PasskeyCredential credential) {
        return baseMapper.insert(credential) > 0;
    }

    @Override
    public List<PasskeyCredential> listByUserId(Long userId) {
        LambdaQueryWrapper<PasskeyCredential> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PasskeyCredential::getUserId, userId)
                .orderByDesc(PasskeyCredential::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public PasskeyCredential getByCredentialIdB64(String credentialIdB64) {
        LambdaQueryWrapper<PasskeyCredential> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PasskeyCredential::getCredentialIdB64, credentialIdB64);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public PasskeyCredential getByUserHandleB64(String userHandleB64) {
        LambdaQueryWrapper<PasskeyCredential> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PasskeyCredential::getUserHandleB64, userHandleB64);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean updateSignCount(Long id, long signCount) {
        PasskeyCredential credential = new PasskeyCredential();
        credential.setId(id);
        credential.setSignCount(signCount);
        credential.setLastUsedTime(LocalDateTime.now());
        return baseMapper.updateById(credential) > 0;
    }

    @Override
    public int updateSignCountCAS(Long id, long expectedSignCount, long newSignCount) {
        LambdaUpdateWrapper<PasskeyCredential> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PasskeyCredential::getId, id)
                .eq(PasskeyCredential::getSignCount, expectedSignCount)
                .set(PasskeyCredential::getSignCount, newSignCount)
                .set(PasskeyCredential::getLastUsedTime, LocalDateTime.now());
        return baseMapper.update(null, wrapper);
    }

    @Override
    public boolean deleteByUserIdAndCredentialId(Long userId, String credentialIdB64) {
        LambdaQueryWrapper<PasskeyCredential> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PasskeyCredential::getUserId, userId)
                .eq(PasskeyCredential::getCredentialIdB64, credentialIdB64);
        return baseMapper.delete(wrapper) > 0;
    }

}
