package com.carlos.core.auth;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 身份断言载荷
 * </p>
 *
 * <p>网关验完令牌后，把登录身份签成一个不可伪造的断言（HTTP 头 {@code X-User-Context}）传给下游，
 * 下游验签后据此重建 SecurityContext。相比「明文注入 {@code X-User-*} 头 + 信任来源 IP」，
 * 断言的完整性由密码学保证，与调用方所在网段无关，因此可以安全地随内部 Feign 调用多跳传播。</p>
 *
 * <p><b>时间字段单位为 epoch 秒</b>（与 JWT 的 iat/exp 约定一致）。注意别与网关的
 * {@code X-Gateway-Timestamp}（epoch 毫秒）混用。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-09-23
 * @see IdentityAssertionCodec
 */
@Data
@Accessors(chain = true)
public class IdentityAssertion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID（必填，缺失视为无效断言）
     */
    private Long uid;

    /**
     * 登录账号
     */
    private String acc;

    /**
     * 角色编码列表（如 ["SUPER_ADMIN"]，不是角色主键）
     */
    private List<String> roles;

    /**
     * 主部门 ID
     */
    private Long dept;

    /**
     * 租户 ID
     */
    private Long ten;

    /**
     * 签发时间（epoch 秒）
     */
    private Long iat;

    /**
     * 过期时间（epoch 秒）
     */
    private Long exp;

}
