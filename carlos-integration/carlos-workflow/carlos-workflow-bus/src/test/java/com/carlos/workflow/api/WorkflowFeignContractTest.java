package com.carlos.workflow.api;

import com.carlos.workflow.ServiceNameConstant;
import com.carlos.workflow.apiimpl.ApiWorkflowDefinitionImpl;
import com.carlos.workflow.apiimpl.ApiWorkflowInstanceImpl;
import com.carlos.workflow.apiimpl.ApiWorkflowTaskImpl;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * Feign 契约一致性测试：守护<b>服务间命名空间</b>（{@code /api/workflow/**}）自洽。
 * </p>
 * <p>
 * 前端（经网关）的管理端点是另一套命名空间（{@code /workflow/**}，见
 * {@code com.carlos.workflow.controller}），由 {@code WorkflowNamespaceBoundaryTest} 守护其边界；
 * 本类只管 Feign 这一侧。
 * </p>
 * <p>
 * 这里检查的四件事都是实际问题，而非形式检查：
 * </p>
 * <ol>
 *   <li><b>contextId 必须唯一</b>：Spring Cloud 用 contextId 区分同一服务的多个 Feign 客户端，
 *       重复会导致客户端互相覆盖（本仓库 carlos-org-api 已占用 13 个，carlos-job-api 1 个）；</li>
 *   <li><b>value 必须是本服务注册名</b>：写错会让调用发到别的服务；</li>
 *   <li><b>Feign 的 path + 方法映射必须与 apiimpl 的 @RequestMapping 完全一致</b>：
 *       二者不一致时，服务间调用 404，且不会有任何编译期提示；</li>
 *   <li><b>实现类不得多挂端点</b>：apiimpl 上任何不在契约接口里的映射，都意味着有接口绕过
 *       {@code /api/**} 的内部定位被单独暴露出去。</li>
 * </ol>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
class WorkflowFeignContractTest {

    /**
     * Feign 接口 → 其实现类（同时也是对前端暴露的 REST 控制器）
     */
    private static final Map<Class<?>, Class<?>> CLIENTS = new LinkedHashMap<>();

    static {
        CLIENTS.put(ApiWorkflowTask.class, ApiWorkflowTaskImpl.class);
        CLIENTS.put(ApiWorkflowInstance.class, ApiWorkflowInstanceImpl.class);
        CLIENTS.put(ApiWorkflowDefinition.class, ApiWorkflowDefinitionImpl.class);
    }

    @Test
    void everyFeignClientDeclaresAServiceNameAndDistinctContextId() {
        List<String> contextIds = new ArrayList<>();
        for (Class<?> client : CLIENTS.keySet()) {
            FeignClient feignClient = AnnotatedElementUtils.findMergedAnnotation(client, FeignClient.class);
            assertThat(feignClient)
                .as("%s 必须声明 @FeignClient", client.getSimpleName())
                .isNotNull();
            assertThat(feignClient.value())
                .as("%s 的 FeignClient 服务名", client.getSimpleName())
                .isEqualTo(ServiceNameConstant.SERVICE_NAME);
            assertThat(feignClient.fallbackFactory())
                .as("%s 必须提供熔断降级工厂", client.getSimpleName())
                .isNotEqualTo(FallbackFactory.class);

            contextIds.add(feignClient.contextId());
        }

        assertThat(contextIds)
            .as("contextId 重复会导致同一服务的多个 Feign 客户端互相覆盖")
            .doesNotHaveDuplicates()
            .doesNotContainNull();
    }

    @Test
    void everyFeignMethodResolvesToTheSameUrlAsItsController() {
        for (Map.Entry<Class<?>, Class<?>> entry : CLIENTS.entrySet()) {
            Class<?> client = entry.getKey();
            Class<?> impl = entry.getValue();

            FeignClient feignClient = AnnotatedElementUtils.findMergedAnnotation(client, FeignClient.class);
            String clientBasePath = feignClient.path();

            RequestMapping implBaseMapping = AnnotatedElementUtils.findMergedAnnotation(impl, RequestMapping.class);
            assertThat(implBaseMapping)
                .as("%s 缺少类级 @RequestMapping", impl.getSimpleName())
                .isNotNull();
            String implBasePath = implBaseMapping.path()[0];

            assertThat(implBasePath)
                .as("%s 与 %s 的基础路径必须一致", impl.getSimpleName(), client.getSimpleName())
                .isEqualTo(clientBasePath);

            for (Method clientMethod : client.getDeclaredMethods()) {
                Method implMethod = implementationOf(impl, clientMethod);
                RequestMapping clientMapping =
                    AnnotatedElementUtils.findMergedAnnotation(clientMethod, RequestMapping.class);
                RequestMapping implMapping =
                    AnnotatedElementUtils.findMergedAnnotation(implMethod, RequestMapping.class);

                assertThat(clientMapping)
                    .as("%s#%s 缺少映射注解", client.getSimpleName(), clientMethod.getName())
                    .isNotNull();
                assertThat(implMapping)
                    .as("%s#%s 缺少映射注解", impl.getSimpleName(), implMethod.getName())
                    .isNotNull();

                String clientUrl = clientBasePath + firstPath(clientMapping);
                String implUrl = implBasePath + firstPath(implMapping);

                assertThat(implUrl)
                    .as("%s#%s 的 URL 必须与 Feign 契约一致", impl.getSimpleName(), implMethod.getName())
                    .isEqualTo(clientUrl);
                assertThat(implMapping.method())
                    .as("%s#%s 的 HTTP 方法必须与 Feign 契约一致", impl.getSimpleName(), implMethod.getName())
                    .isEqualTo(clientMapping.method());
            }
        }
    }

    @Test
    void everyImplEndpointIsDeclaredInTheFeignContract() {
        for (Map.Entry<Class<?>, Class<?>> entry : CLIENTS.entrySet()) {
            Class<?> client = entry.getKey();
            Class<?> impl = entry.getValue();

            List<String> extra = new ArrayList<>();
            for (Method implMethod : impl.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(implMethod, RequestMapping.class) == null) {
                    continue;
                }
                if (!declaredIn(client, implMethod)) {
                    extra.add(impl.getSimpleName() + "#" + implMethod.getName());
                }
            }

            assertThat(extra)
                .as("%s 上挂着契约接口之外的端点：管理端接口必须落在 /workflow/** 的 controller 里，"
                    + "不能借 /api/** 的内部定位暴露出去", impl.getSimpleName())
                .isEmpty();
        }
    }

    private static boolean declaredIn(Class<?> client, Method implMethod) {
        try {
            client.getMethod(implMethod.getName(), implMethod.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Method implementationOf(Class<?> impl, Method clientMethod) {
        try {
            return impl.getMethod(clientMethod.getName(), clientMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                impl.getSimpleName() + " 未实现 " + clientMethod.getName() + "，接口与实现已脱节", e);
        }
    }

    private static String firstPath(RequestMapping mapping) {
        return mapping.path().length == 0 ? "" : mapping.path()[0];
    }

}
