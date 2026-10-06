package com.carlos.auth.mfa.service;

import cn.hutool.core.util.RandomUtil;
import com.carlos.auth.mfa.manager.MfaManager;
import com.carlos.auth.mfa.pojo.entity.MfaRecoveryCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MFA备用恢复码服务
 *
 * <p>管理MFA备用恢复码的生成、验证和使用。</p>
 *
 * <p><strong>分层职责：</strong></p>
 * <ul>
 *   <li>Service：专注业务逻辑，处理恢复码生成策略、验证流程</li>
 *   <li>Manager：专注数据访问，处理恢复码的增删改查（通过 {@link MfaManager}）</li>
 * </ul>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-02-26
 * @see com.carlos.auth.mfa.manager.MfaManager
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MfaRecoveryCodeService {

    private final MfaManager mfaManager;

    /**
     * 生成备用恢复码
     *
     * @return 10组8位随机码列表
     */
    public List<String> generateRecoveryCodes() {
        List<String> codes = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            // 生成8位随机码（字母数字混合）
            String code = RandomUtil.randomString(8).toUpperCase();
            codes.add(code);
        }

        log.debug("Generated 10 recovery codes");
        return codes;
    }

    /**
     * 保存用户的备用恢复码
     *
     * @param userId 用户ID
     * @param recoveryCodes 恢复码列表
     */
    public void saveRecoveryCodes(Long userId, List<String> recoveryCodes) {
        if (userId == null || recoveryCodes == null || recoveryCodes.isEmpty()) {
            return;
        }

        // 清除旧的恢复码
        mfaManager.clearRecoveryCodes(userId);

        // 构建实体列表
        List<MfaRecoveryCode> entities = recoveryCodes.stream()
            .map(code -> {
                MfaRecoveryCode entity = new MfaRecoveryCode();
                entity.setUserId(userId);
                entity.setCode(code);
                entity.setUsed(false);
                entity.setCreateTime(LocalDateTime.now());
                return entity;
            })
            .collect(Collectors.toList());

        // 批量保存
        mfaManager.saveRecoveryCodes(entities);

        log.info("Saved {} recovery codes for user: {}", recoveryCodes.size(), userId);
    }

    /**
     * 使用恢复码
     *
     * @param userId 用户ID
     * @param code 恢复码
     * @return true-使用成功，false-恢复码无效或已使用
     */
    public boolean useRecoveryCode(Long userId, String code) {
        if (userId == null || code == null) {
            return false;
        }

        boolean success = mfaManager.verifyAndUseRecoveryCode(userId, code.toUpperCase());

        if (success) {
            log.info("Recovery code used for user: {}", userId);
        } else {
            log.warn("Invalid or already used recovery code for user: {}", userId);
        }

        return success;
    }

    /**
     * 验证恢复码（不标记为已使用）
     *
     * @param userId 用户ID
     * @param code 恢复码
     * @return true-验证通过，false-恢复码无效或已使用
     */
    public boolean verifyRecoveryCode(Long userId, String code) {
        if (userId == null || code == null) {
            return false;
        }

        List<MfaRecoveryCode> codes = mfaManager.getRecoveryCodesByUserId(userId);
        return codes.stream()
            .anyMatch(c -> c.getCode().equalsIgnoreCase(code) && !c.getUsed());
    }

    /**
     * 获取用户的未使用恢复码
     *
     * @param userId 用户ID
     * @return 未使用的恢复码列表
     */
    public List<String> getUnusedRecoveryCodes(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        List<MfaRecoveryCode> codes = mfaManager.getRecoveryCodesByUserId(userId);
        return codes.stream()
            .filter(c -> !c.getUsed())
            .map(MfaRecoveryCode::getCode)
            .collect(Collectors.toList());
    }

    /**
     * 检查恢复码是否可用
     *
     * @param userId 用户ID
     * @return true-有可用恢复码，false-没有可用恢复码
     */
    public boolean hasAvailableRecoveryCode(Long userId) {
        if (userId == null) {
            return false;
        }

        List<MfaRecoveryCode> codes = mfaManager.getRecoveryCodesByUserId(userId);
        return codes.stream().anyMatch(c -> !c.getUsed());
    }

    /**
     * 清除用户的所有恢复码
     *
     * @param userId 用户ID
     */
    public void clearRecoveryCodes(Long userId) {
        if (userId == null) {
            return;
        }

        mfaManager.clearRecoveryCodes(userId);
        log.info("Cleared all recovery codes for user: {}", userId);
    }
}
