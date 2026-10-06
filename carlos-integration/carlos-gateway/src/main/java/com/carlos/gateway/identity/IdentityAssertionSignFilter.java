package com.carlos.gateway.identity;

import com.carlos.core.auth.AuthConstant;
import com.carlos.core.auth.IdentityAssertion;
import com.carlos.core.auth.IdentityAssertionCodec;
import com.carlos.core.auth.IdentityAssertionProperties;
import com.carlos.core.auth.UserContext;
import com.carlos.core.constant.HttpHeadersConstant;
import com.carlos.gateway.config.GlobalFilterOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * <p>
 * 身份断言签发过滤器
 * </p>
 *
 * <p>认证通过后，把 {@link UserContext} 签成 {@code X-User-Context} 断言（详见
 * {@link IdentityAssertionCodec}），下游凭共享密钥验签后重建登录身份。相比「明文注入
 * {@code X-User-*} + 信任来源 IP」，签名与调用方网段无关，因此可以安全地随内部 Feign 调用
 * 多跳传播（{@code FeignRequestInterceptor} 会转发所有 {@code x-} 前缀的头）。</p>
 *
 * <p><b>开关在请求时读取</b>（{@link IdentityAssertionProperties#isSignEnabled()}），不用
 * {@code @ConditionalOnProperty} —— 条件化 Bean 不随配置刷新重建，会让灰度期出现「改了配置
 * 不生效」的假象，也让单侧回滚失去即时性。</p>
 *
 * <p><b>失败一律降级、不抛异常</b>：签发失败只是本次不注入断言（下游降级为匿名，并留下日志），
 * 绝不让一次身份签发故障演变成网关整体不可用 —— 这正是原先 IP 白名单方案最糟的失败模式。</p>
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-09-23
 */
@Slf4j
@RequiredArgsConstructor
public class IdentityAssertionSignFilter implements GlobalFilter, Ordered {

    private final IdentityAssertionProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isSignEnabled()) {
            return chain.filter(exchange);
        }

        if (!properties.hasSecret()) {
            log.error("[身份断言] sign-enabled=true 但未配置 carlos.identity-assertion.secret，"
                + "本次不签发断言，下游将降级为匿名。请在 Nacos 共享配置 common.yml 中补齐密钥");
            return chain.filter(exchange);
        }

        Object attribute = exchange.getAttribute(AuthConstant.USER_CONTEXT);
        if (!(attribute instanceof UserContext userContext)) {
            // 白名单路径、或认证未通过：没有身份可签，下游自然降级为匿名（符合预期）
            log.debug("No user context in exchange, skip signing identity assertion");
            return chain.filter(exchange);
        }

        String assertion = buildAssertion(userContext);
        if (assertion == null) {
            return chain.filter(exchange);
        }

        ServerHttpRequest mutated = exchange.getRequest().mutate()
            .header(HttpHeadersConstant.X_USER_CONTEXT, assertion)
            .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /**
     * 构建并签名身份断言
     *
     * @return 断言头值；无法构建时返回 null
     */
    private String buildAssertion(UserContext userContext) {
        Long userId = parseLongText(userContext.getUserId());
        if (userId == null) {
            log.warn("[身份断言] 用户上下文缺少合法的 userId，跳过签发");
            return null;
        }

        long issuedAt = Instant.now().getEpochSecond();
        IdentityAssertion assertion = new IdentityAssertion()
            .setUid(userId)
            .setAcc(userContext.getAccount())
            .setRoles(toRoleCodes(userContext.getRoleIds()))
            .setDept(lenientLong(userContext::getDepartmentId, "departmentId"))
            .setTen(lenientLong(userContext::getTenantId, "tenantId"))
            .setIat(issuedAt)
            .setExp(issuedAt + properties.getTtl().toSeconds());

        try {
            return IdentityAssertionCodec.encode(assertion, properties.getSecret());
        } catch (RuntimeException e) {
            log.error("[身份断言] 签发失败，本次不注入断言", e);
            return null;
        }
    }

    /**
     * 角色标识列表
     *
     * <p>网关的 {@code roleIds} 实际承载的是角色<b>编码</b>（如 {@code SUPER_ADMIN}），
     * 因此原样取 {@code toString()}，不做数值解析 —— 这与下游「角色标识即编码」的契约一致。</p>
     */
    private List<String> toRoleCodes(Set<Serializable> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<String> roleCodes = new ArrayList<>(roleIds.size());
        for (Serializable roleId : roleIds) {
            if (roleId != null) {
                roleCodes.add(roleId.toString());
            }
        }
        return roleCodes;
    }

    /**
     * 宽松解析字符串形式的数字
     */
    private Long parseLongText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 宽松读取 Long 字段
     *
     * <p>{@link UserContext#getTenantId()} / {@link UserContext#getDepartmentId()} 内部是
     * {@code Long.valueOf(String)}，遇到非数字值会抛异常。这里降级为 null 并留日志，
     * 而不是让整条身份链失败。</p>
     */
    private Long lenientLong(Supplier<Long> accessor, String fieldName) {
        try {
            return accessor.get();
        } catch (RuntimeException e) {
            log.warn("[身份断言] 字段 {} 非法，按空处理：{}", fieldName, e.getMessage());
            return null;
        }
    }

    @Override
    public int getOrder() {
        return GlobalFilterOrder.IDENTITY_SIGN_ORDER;
    }

}
