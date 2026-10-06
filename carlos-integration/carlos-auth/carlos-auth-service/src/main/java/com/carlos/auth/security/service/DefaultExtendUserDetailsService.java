package com.carlos.auth.security.service;

import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.provider.UserProvider;
import com.carlos.core.auth.LoginUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * 默认扩展用户详情服务实现
 *
 * <p>本实现委托给 {@link UserProvider} 获取用户信息，仅用于开发和测试环境。
 * 生产环境必须提供自定义实现。</p>
 *
 * <h3>与 UserProvider 的关系：</h3>
 * <p>本实现内部使用注入的 {@link UserProvider} 作为数据源，
 * 确保用户数据的一致性。实际测试用户由 {@link com.carlos.auth.provider.DefaultUserProvider} 维护。</p>
 *
 * <h3>生产环境迁移指南：</h3>
 * <pre>{@code
 * @Service
 * @Primary
 * public class MyUserDetailsService implements ExtendUserDetailsService {
 *
 *     @Autowired
 *     private UserRepository userRepository;
 *
 *     @Override
 *     public UserDetails loadUserByUsername(String username) {
 *         User user = userRepository.findByUsername(username)
 *             .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
 *
 *         return User.builder()
 *             .username(user.getUsername())
 *             .password(user.getPassword())
 *             .roles(user.getRoles().toArray(new String[0]))
 *             .build();
 *     }
 *
 *     @Override
 *     public LoginUserInfo loadLoginUserInfo(String username) {
 *         User user = userRepository.findByUsername(username)
 *             .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
 *
 *         LoginUserInfo info = new LoginUserInfo();
 *         info.setId(user.getId());
 *         info.setAccount(user.getUsername());
 *         info.setPassword(user.getPassword());
 *         info.setEnable(user.isEnabled());
 *         info.setRoleIds(new HashSet<>(user.getRoleIds()));
 *         return info;
 *     }
 * }
 * }</pre>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-02-22
 * @see ExtendUserDetailsService
 * @see UserProvider
 */
@Slf4j
@Profile("!prod")
public class DefaultExtendUserDetailsService implements ExtendUserDetailsService {

    /**
     * 密码编码器
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户提供者（作为数据源）
     */
    private final UserProvider userProvider;

    /**
     * 构造函数
     *
     * @param passwordEncoder 密码编码器
     * @param userProvider 用户提供者
     */
    public DefaultExtendUserDetailsService(PasswordEncoder passwordEncoder, UserProvider userProvider) {
        this.passwordEncoder = passwordEncoder;
        this.userProvider = userProvider;

        log.warn("=================================================================");
        log.warn(" Using DefaultExtendUserDetailsService - FOR DEVELOPMENT ONLY!  ");
        log.warn("=================================================================");
        log.warn("Please implement ExtendUserDetailsService for production use!");
        log.warn("=================================================================");
    }

    /**
     * 根据用户名加载用户详情
     *
     * <p>支持通过用户名、邮箱或手机号查询。</p>
     *
     * @param username 用户名/邮箱/手机号
     * @return UserDetails 用户详情
     * @throws UsernameNotFoundException 用户不存在时抛出
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 使用 UserProvider 获取用户信息
        UserInfo userInfo = userProvider.loadUserByIdentifier(username);

        if (userInfo == null) {
            log.warn("User not found: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 转换为 Spring Security 的 UserDetails
        return User.builder()
            .username(userInfo.getUsername())
            .password(userInfo.getPassword())
            .roles(userInfo.getRoleCodes() != null
                ? userInfo.getRoleCodes().toArray(new String[0])
                : new String[0])
            .accountLocked(userInfo.isAccountLocked())
            .disabled(!userInfo.isAccountEnabled())
            .build();
    }

    /**
     * 加载用户业务信息
     *
     * <p>用于 JWT Token 增强，将用户信息添加到 Token 中。</p>
     *
     * @param username 用户名/邮箱/手机号
     * @return LoginUserInfo 登录用户信息
     */
    @Override
    public LoginUserInfo loadLoginUserInfo(String username) {
        UserInfo userInfo = userProvider.loadUserByIdentifier(username);

        if (userInfo == null) {
            log.debug("No user info found for: {}", username);
            return null;
        }

        // 转换为 LoginUserInfo
        LoginUserInfo info = new LoginUserInfo();
        info.setId(userInfo.getUserId());
        info.setAccount(userInfo.getUsername());
        info.setPassword(userInfo.getPassword());
        info.setEnable(userInfo.isAccountEnabled());
        info.setClientId(userInfo.getTenantId()); // 多租户场景使用 tenantId 作为 clientId
        info.setDepartmentId(userInfo.getDeptId());

        // 转换角色ID
        if (userInfo.getRoleCodes() != null) {
            Set<Serializable> roleIds = new HashSet<>();
            for (String roleCode : userInfo.getRoleCodes()) {
                // 简单映射：ADMIN -> 1, USER -> 2，其他根据角色名哈希
                if ("ADMIN".equals(roleCode)) {
                    roleIds.add(1L);
                } else if ("USER".equals(roleCode)) {
                    roleIds.add(2L);
                } else {
                    roleIds.add((long) roleCode.hashCode());
                }
            }
            info.setRoleIds(roleIds);
        }

        return info;
    }

}
