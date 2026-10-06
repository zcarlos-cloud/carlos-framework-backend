package com.carlos.auth.mfa.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.mfa.manager.MfaManager;
import com.carlos.auth.mfa.mapper.MfaRecoveryCodeMapper;
import com.carlos.auth.mfa.mapper.TrustedDeviceMapper;
import com.carlos.auth.mfa.pojo.entity.MfaRecoveryCode;
import com.carlos.auth.mfa.pojo.entity.TrustedDevice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MFA Manager 实现类
 *
 * <p>实现 MFA 相关的数据访问操作，处理恢复码和可信设备的增删改查。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaManagerImpl implements MfaManager {

    private final MfaRecoveryCodeMapper recoveryCodeMapper;
    private final TrustedDeviceMapper trustedDeviceMapper;

    @Override
    public boolean saveRecoveryCode(MfaRecoveryCode recoveryCode) {
        return recoveryCodeMapper.insert(recoveryCode) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRecoveryCodes(List<MfaRecoveryCode> recoveryCodes) {
        if (recoveryCodes == null || recoveryCodes.isEmpty()) {
            return false;
        }
        for (MfaRecoveryCode code : recoveryCodes) {
            int affected = recoveryCodeMapper.insert(code);
            if (affected <= 0) {
                log.warn("Insert recovery code failed for user: {}", code.getUserId());
                throw AuthErrorCode.AUTH_MFA_SETUP_FAILED.exception("恢复码保存失败，userId: %s", code.getUserId());
            }
        }
        return true;
    }

    @Override
    public List<MfaRecoveryCode> getRecoveryCodesByUserId(Long userId) {
        LambdaQueryWrapper<MfaRecoveryCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MfaRecoveryCode::getUserId, userId)
            .eq(MfaRecoveryCode::getUsed, false)
            .orderByDesc(MfaRecoveryCode::getCreateTime);
        return recoveryCodeMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyAndUseRecoveryCode(Long userId, String code) {
        LambdaQueryWrapper<MfaRecoveryCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MfaRecoveryCode::getUserId, userId)
            .eq(MfaRecoveryCode::getCode, code)
            .eq(MfaRecoveryCode::getUsed, false);

        MfaRecoveryCode recoveryCode = recoveryCodeMapper.selectOne(wrapper);
        if (recoveryCode == null) {
            return false;
        }

        // 标记为已使用
        recoveryCode.setUsed(true);
        recoveryCode.setUsedTime(LocalDateTime.now());
        return recoveryCodeMapper.updateById(recoveryCode) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearRecoveryCodes(Long userId) {
        LambdaQueryWrapper<MfaRecoveryCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MfaRecoveryCode::getUserId, userId);
        return recoveryCodeMapper.delete(wrapper) > 0;
    }

    @Override
    public boolean saveTrustedDevice(TrustedDevice trustedDevice) {
        return trustedDeviceMapper.insert(trustedDevice) > 0;
    }

    @Override
    public TrustedDevice getTrustedDevice(Long userId, String deviceFingerprint) {
        LambdaQueryWrapper<TrustedDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrustedDevice::getUserId, userId)
            .eq(TrustedDevice::getDeviceFingerprint, deviceFingerprint);
        return trustedDeviceMapper.selectOne(wrapper);
    }

    @Override
    public List<TrustedDevice> getTrustedDevicesByUserId(Long userId) {
        LambdaQueryWrapper<TrustedDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrustedDevice::getUserId, userId)
            .orderByDesc(TrustedDevice::getCreateTime);
        return trustedDeviceMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTrustedDevice(Long userId, Long deviceId) {
        LambdaQueryWrapper<TrustedDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrustedDevice::getUserId, userId)
            .eq(TrustedDevice::getId, deviceId);
        return trustedDeviceMapper.delete(wrapper) > 0;
    }
}
