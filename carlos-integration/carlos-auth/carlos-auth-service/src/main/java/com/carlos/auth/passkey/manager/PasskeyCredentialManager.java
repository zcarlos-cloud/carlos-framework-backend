package com.carlos.auth.passkey.manager;

import com.carlos.auth.passkey.pojo.entity.PasskeyCredential;
import com.carlos.datasource.base.BaseService;

import java.util.List;

/**
 * <p>
 * Passkey 凭据查询封装接口
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public interface PasskeyCredentialManager extends BaseService<PasskeyCredential> {

    /**
     * 保存凭据
     *
     * @param credential 凭据实体
     * @return true-成功
     */
    boolean saveCredential(PasskeyCredential credential);

    /**
     * 根据用户ID查询凭据列表
     *
     * @param userId 用户ID
     * @return 凭据列表
     */
    List<PasskeyCredential> listByUserId(Long userId);

    /**
     * 根据 base64url 凭据ID查询凭据
     *
     * @param credentialIdB64 凭据ID（base64url）
     * @return 凭据
     */
    PasskeyCredential getByCredentialIdB64(String credentialIdB64);

    /**
     * 根据用户句柄查询凭据
     *
     * @param userHandleB64 用户句柄（base64url）
     * @return 凭据
     */
    PasskeyCredential getByUserHandleB64(String userHandleB64);

    /**
     * 更新签名计数器和最后使用时间
     *
     * @param id        凭据ID
     * @param signCount 新签名计数器
     * @return true-成功
     */
    boolean updateSignCount(Long id, long signCount);

    /**
     * CAS 方式更新签名计数器（乐观锁，防止计数器倒退）
     *
     * @param id              凭据ID
     * @param expectedSignCount 期望的旧签名计数器
     * @param newSignCount     新签名计数器
     * @return 更新行数
     */
    int updateSignCountCAS(Long id, long expectedSignCount, long newSignCount);

    /**
     * 删除用户指定的凭据
     *
     * @param userId            用户ID
     * @param credentialIdB64   凭据ID（base64url）
     * @return true-成功
     */
    boolean deleteByUserIdAndCredentialId(Long userId, String credentialIdB64);

}
