package com.carlos.auth.mfa.manager;

import com.carlos.auth.mfa.pojo.entity.MfaRecoveryCode;
import com.carlos.auth.mfa.pojo.entity.TrustedDevice;

import java.util.List;

/**
 * MFA Manager 接口
 *
 * <p>数据查询封装层，继承 BaseService，处理 MFA 相关的数据持久化操作。</p>
 *
 * <p><strong>与 Service 层的区别：</strong></p>
 * <ul>
 *   <li>Manager：专注数据访问，处理增删改查等原子操作</li>
 *   <li>Service：专注业务逻辑，处理业务流程串联</li>
 * </ul>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 * @see com.carlos.auth.mfa.service.MfaService
 */
public interface MfaManager {

    /**
     * 保存恢复码
     *
     * @param recoveryCode 恢复码实体
     * @return 是否成功
     */
    boolean saveRecoveryCode(MfaRecoveryCode recoveryCode);

    /**
     * 批量保存恢复码
     *
     * @param recoveryCodes 恢复码列表
     * @return 是否成功
     */
    boolean saveRecoveryCodes(List<MfaRecoveryCode> recoveryCodes);

    /**
     * 获取用户的恢复码列表
     *
     * @param userId 用户ID
     * @return 恢复码列表
     */
    List<MfaRecoveryCode> getRecoveryCodesByUserId(Long userId);

    /**
     * 验证并使用恢复码
     *
     * @param userId 用户ID
     * @param code 恢复码
     * @return 是否验证成功
     */
    boolean verifyAndUseRecoveryCode(Long userId, String code);

    /**
     * 清除用户的所有恢复码
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean clearRecoveryCodes(Long userId);

    /**
     * 保存可信设备
     *
     * @param trustedDevice 可信设备实体
     * @return 是否成功
     */
    boolean saveTrustedDevice(TrustedDevice trustedDevice);

    /**
     * 获取用户的可信设备
     *
     * @param userId 用户ID
     * @param deviceFingerprint 设备指纹
     * @return 可信设备
     */
    TrustedDevice getTrustedDevice(Long userId, String deviceFingerprint);

    /**
     * 获取用户的所有可信设备
     *
     * @param userId 用户ID
     * @return 可信设备列表
     */
    List<TrustedDevice> getTrustedDevicesByUserId(Long userId);

    /**
     * 移除可信设备
     *
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @return 是否成功
     */
    boolean removeTrustedDevice(Long userId, Long deviceId);
}
