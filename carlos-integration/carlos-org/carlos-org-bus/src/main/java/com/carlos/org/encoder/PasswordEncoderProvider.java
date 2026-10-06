package com.carlos.org.encoder;

/**
 * <p>
 * 密码加密提供者接口
 * </p>
 *
 * <p>定义密码编码和匹配的标准操作。业务模块可通过实现此接口来替换默认的密码加密策略。</p>
 *
 * <p><strong>使用场景：</strong></p>
 * <ul>
 *   <li>新增用户时，对前端传入的明文密码进行编码后存储</li>
 *   <li>重置密码时，对新密码进行编码后存储</li>
 *   <li>修改密码时，校验旧密码是否匹配</li>
 * </ul>
 *
 * <p><strong>扩展方式：</strong></p>
 * <pre>{@code
 * @Component
 * public class MyPasswordEncoderProvider implements PasswordEncoderProvider {
 *     public String encode(String rawPassword) {
 *         // 自定义编码逻辑
 *     }
 *     public boolean matches(String rawPassword, String encodedPassword) {
 *         // 自定义匹配逻辑
 *     }
 * }
 * }</pre>
 *
 * @author Carlos
 * @since 3.0.0
 */
public interface PasswordEncoderProvider {

    /**
     * 对明文密码进行编码
     *
     * <p>编码后的密码将持久化到数据库中。编码必须是单向的（不可逆）。</p>
     *
     * @param rawPassword 明文密码，不为空
     * @return 编码后的密码密文
     */
    String encode(String rawPassword);

    /**
     * 校验明文密码是否与已编码的密码匹配
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 已编码的密码密文
     * @return 是否匹配
     */
    boolean matches(String rawPassword, String encodedPassword);

}
