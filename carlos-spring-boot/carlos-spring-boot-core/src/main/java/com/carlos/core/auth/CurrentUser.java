package com.carlos.core.auth;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.io.Serializable;

/**
 * <p>
 * 当前登录用户信息
 * </p>
 *
 * @author carlos
 * @date 2021/6/16 13:37
 */
public class CurrentUser {

    private static final TransmittableThreadLocal<UserContext> USER_INFO = new TransmittableThreadLocal<>();

    private CurrentUser() {
    }

    public static UserContext getCurrentUser() {
        return USER_INFO.get();
    }

    public static Serializable getUserId() {
        UserContext currentUser = getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        return currentUser.getUserId();
    }

    public static void clear() {
        USER_INFO.remove();
    }


    public static void setCurrentUser(UserContext user) {
        if (user != null) {
            USER_INFO.set(user);
        }
    }
}
