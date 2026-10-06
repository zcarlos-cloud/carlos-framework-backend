package com.carlos.auth.security;

import com.carlos.core.auth.LoginUserInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * 登录用户信息
 * </p>
 *
 * @author carlos
 * @date 2021/12/8 10:50
 */
@Data
@NoArgsConstructor
public class SecurityUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Serializable id;
    /**
     * 客户端id
     */
    private Serializable clientId;
    /**
     * 用户名
     */
    private String account;
    /**
     * 密码
     */
    private String password;
    /**
     * 是否可用
     */
    private boolean enable;

    /**
     * 权限数据
     */
    private Collection<SimpleGrantedAuthority> authorities;

    public SecurityUser(LoginUserInfo user) {
        this.id = user.getId();
        this.clientId = user.getClientId();
        this.account = user.getAccount();
        this.password = user.getPassword();
        this.enable = user.getEnable();
        // 权限赋值
        Optional.ofNullable(user.getRoleIds())
            .ifPresent(i -> this.authorities =
                i.stream()
                    .map(role -> new SimpleGrantedAuthority(String.valueOf(role)))
                    .collect(Collectors.toList())
            );
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return this.password;
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        return this.account;
    }

    /**
     * 账户是否过期
     *
     * @return boolean true 代表用户可用
     * @author carlos
     * @date 2021/3/4 14:42
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        // 用户被删除表示已经过期
        return true;
    }

    /**
     * 指定用户是否解锁,锁定的用户无法进行身份验证
     */
    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 指示是否已过期的用户的凭据(密码),过期的凭据防止认证
     */
    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 是否可用 ,禁用的用户不能身份验证
     */
    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return this.enable;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

}
