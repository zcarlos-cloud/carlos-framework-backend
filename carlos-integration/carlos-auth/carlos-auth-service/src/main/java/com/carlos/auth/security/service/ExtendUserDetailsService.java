package com.carlos.auth.security.service;

import com.carlos.core.auth.LoginUserInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * 扩展的用户详情服务接口
 *
 * <p>本接口继承自 Spring Security 的 UserDetailsService，增加了加载用户业务信息的能力。
 * 应用可以实现此接口来提供自定义的用户加载逻辑。</p>
 *
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>加载用户认证信息（UserDetails）- 用于登录验证</li>
 *   <li>加载用户业务信息（LoginUserInfo）- 用于 Token 增强</li>
 *   <li>支持用户角色和权限加载</li>
 * </ul>
 *
 * <h3>使用方式：</h3>
 *
 * <h4>1. 实现接口</h4>
 * <pre>{@code
 * @Service
 * public class MyUserDetailsService implements ExtendUserDetailsService {
 *
 *     @Autowired
 *     private UserMapper userMapper;
 *
 *     @Override
 *     public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
 *         // 1. 查询用户
 *         UserDO user = userMapper.selectByUsername(username);
 *         if (user == null) {
 *             throw new UsernameNotFoundException("用户不存在: " + username);
 *         }
 *
 *         // 2. 查询权限
 *         List<GrantedAuthority> authorities = loadAuthorities(user.getId());
 *
 *         // 3. 构建 UserDetails
 *         return User.builder()
 *             .username(user.getUsername())
 *             .password(user.getPassword())
 *             .authorities(authorities)
 *             .accountExpired(!user.isAccountNonExpired())
 *             .accountLocked(!user.isAccountNonLocked())
 *             .credentialsExpired(!user.isCredentialsNonExpired())
 *             .disabled(!user.isEnabled())
 *             .build();
 *     }
 *
 *     @Override
 *     public LoginUserInfo loadLoginUserInfo(String username) {
 *         UserDO user = userMapper.selectByUsername(username);
 *         if (user == null) {
 *             return null;
 *         }
 *
 *         LoginUserInfo info = new LoginUserInfo();
 *         info.setId(user.getId());
 *         info.setAccount(user.getUsername());
 *         info.setPassword(user.getPassword());
 *         info.setEnable(user.isEnabled());
 *         info.setRoleIds(loadRoleIds(user.getId()));
 *         info.setClientId(user.getTenantId()); // 多租户场景
 *
 *         return info;
 *     }
 * }
 * }</pre>
 *
 * <h4>2. 配置生效</h4>
 * <p>实现类被注册为 Spring Bean 后会自动生效，替换默认实现。</p>
 *
 * <h3>默认实现：</h3>
 * <p>如果没有自定义实现，使用 {@link DefaultExtendUserDetailsService}，
 * 该实现仅在内存中创建一个测试用户，仅用于开发和测试。</p>
 *
 * <p><strong>警告：</strong>生产环境必须提供自定义实现！</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-02-22
 * @see UserDetailsService
 * @see LoginUserInfo
 */
public interface ExtendUserDetailsService extends UserDetailsService {

    /**
     * 根据用户名加载用户信息
     *
     * <p>这是 Spring Security 的标准方法，用于用户认证。</p>
     *
     * <h3>实现要求：</h3>
     * <ul>
     *   <li>必须返回完整的 UserDetails 对象</li>
     *   <li>密码必须使用配置的 PasswordEncoder 加密</li>
     *   <li>如果用户不存在，必须抛出 UsernameNotFoundException</li>
     * </ul>
     *
     * @param username 用户名（通常是账号、手机号或邮箱）
     * @return UserDetails 用户详情，包含用户名、密码、权限等
     * @throws UsernameNotFoundException 当用户不存在时抛出
     */
    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    /**
     * 加载用户业务信息
     *
     * <p>用于 Token 增强，将用户信息添加到 JWT Token 中。</p>
     *
     * <h3>返回信息用途：</h3>
     * <ul>
     *   <li>user_id - 用户唯一标识</li>
     *   <li>account - 用户账号</li>
     *   <li>role_ids - 角色列表，用于权限验证</li>
     *   <li>client_id/tenant_id - 租户标识，用于多租户场景</li>
     *   <li>department_id - 部门标识，用于数据权限</li>
     * </ul>
     *
     * <h3>实现建议：</h3>
     * <p>此方法会被频繁调用（每次生成 Token 时），建议：</p>
     * <ul>
     *   <li>使用缓存（如 Redis）缓存用户信息</li>
     *   <li>只返回必要的信息，避免数据量过大</li>
     *   <li>如果不需要在 Token 中包含用户信息，可以返回 null</li>
     * </ul>
     *
     * @param username 用户名
     * @return LoginUserInfo 登录用户信息，如果不需要则返回 null
     */
    LoginUserInfo loadLoginUserInfo(String username);

    /**
     * 将 LoginUserInfo 转换为 UserDetails（默认实现）
     *
     * <p>提供从 LoginUserInfo 到 UserDetails 的默认转换逻辑。</p>
     * <p>如果业务中两者字段一致，可以直接使用此默认实现。</p>
     *
     * @param loginUserInfo 登录用户信息
     * @return UserDetails Spring Security 用户详情
     */
    default UserDetails convertToUserDetails(LoginUserInfo loginUserInfo) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 转换角色 ID 为权限
        if (loginUserInfo.getRoleIds() != null) {
            for (java.io.Serializable roleId : loginUserInfo.getRoleIds()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleId));
            }
        }

        return User.builder()
            .username(loginUserInfo.getAccount())
            .password(loginUserInfo.getPassword())
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(!loginUserInfo.getEnable())
            .credentialsExpired(false)
            .disabled(!loginUserInfo.getEnable())
            .build();
    }
}
