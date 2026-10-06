package com.carlos.core.auth;

/**
 * <p>
 * 统一认证相关常量
 * </p>
 *
 * @author carlos
 * @date 2021/12/8 10:38
 */
public interface AuthConstant {

    /**
     * 用户上下文在 exchange attributes 中的键名
     */
    String USER_CONTEXT = "userContext";

    /**
     * JWT存储权限前缀
     */
    String AUTHORITY_PREFIX = "ROLE_";

    /**
     * JWT存储权限属性
     */
    String AUTHORITY_CLAIM_NAME = "authorities";

    /**
     * 认证信息Http请求头
     */
    String TOKEN_HEADER = "Authorization";

    /**
     * token前缀 后面有个空格
     */
    String TOKEN_PREFIX = "Bearer ";

    // ==================== 已删除：身份头常量（2026-09-23） ====================
    // 这里原有 9 个小写下划线写法的身份头常量：user_id / user_account / user_token / user_phone /
    // dept_id / dept_ids / role_id / role_ids / tenant_id。
    //
    // 它们是「网关把身份降级成明文头传给下游」那套方案的遗留：唯一写入方是
    // OAuth2AuthenticationFilter.injectUserContext（已删除），而全仓**没有任何读取方** ——
    // 下游真正消费的是 HttpHeadersConstant 里的 X-User-* 系列。留着只会制造「两套命名并存、
    // 到底哪套生效」的困惑（排查时极易被误导）。
    //
    // 现在身份以 X-User-Context 签名断言传递，详见 HttpHeadersConstant.X_USER_CONTEXT。

    /**
     * 客户端id
     */
    String APP_ID = "X-App-id";
    /**
     * APP-KEY
     */
    String APP_KEY = "X-App-Key";
    /**
     * APP-NAME
     */
    String APP_NAME = "X-App-name";

}
