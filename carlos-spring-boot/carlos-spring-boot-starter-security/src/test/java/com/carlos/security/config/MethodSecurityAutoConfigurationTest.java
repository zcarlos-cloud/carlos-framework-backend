package com.carlos.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 方法级安全 Bean 注册测试
 *
 * <p>框架层必须靠 {@code @Bean} 注册（不能用 {@code @Component}）——starter 不参与宿主应用的
 * 组件扫描，类上加 {@code @Component} 会漏注册，且只在运行期 {@code @PreAuthorize("@ps.xxx")}
 * 解析时才会暴露。本测试直接加载做注册的那段配置，确保 {@code ps} 与 {@code permissionEvaluator}
 * 确实在容器里。</p>
 *
 * @author carlos
 */
class MethodSecurityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(ResourceServerAutoConfiguration.MethodSecurityConfig.class);

    @Test
    @DisplayName("应注册 ps 多维鉴权 Bean（@PreAuthorize 的 @ps 依赖它）")
    void should_register_ps_bean() {
        runner.run(context -> assertThat(context)
            .hasBean("ps")
            .getBean("ps").isInstanceOf(com.carlos.security.permission.PermissionChecker.class));
    }

    @Test
    @DisplayName("应注册 permissionEvaluator Bean（@PreAuthorize 的 hasPermission 依赖它）")
    void should_register_permission_evaluator_bean() {
        runner.run(context -> assertThat(context)
            .hasBean("permissionEvaluator")
            .getBean("permissionEvaluator")
            .isInstanceOf(com.carlos.security.permission.evaluator.CarlosPermissionEvaluator.class));
    }
}
