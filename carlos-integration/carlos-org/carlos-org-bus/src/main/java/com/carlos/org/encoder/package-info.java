/**
 * <p>
 * 密码加密提供者接口与默认实现，允许业务模块替换密码编码策略。
 * </p>
 *
 * <p>默认实现 {@link com.carlos.org.encoder.DefaultPasswordEncoderProvider}
 * 通过 Feign 调用认证中心（carlos-auth）完成密码编码与匹配，
 * 支持通过 {@code CARLOS_AUTH_SERVICE_URL} 环境变量配置直连地址。</p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.org.encoder;
