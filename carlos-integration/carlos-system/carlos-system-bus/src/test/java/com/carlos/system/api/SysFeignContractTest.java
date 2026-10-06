package com.carlos.system.api;

import com.carlos.system.ServiceNameConstant;
import com.carlos.system.configration.apiimpl.ApiSysConfigImpl;
import com.carlos.system.dict.apiimpl.ApiDictImpl;
import com.carlos.system.menu.apiimpl.ApiSysMenuImpl;
import com.carlos.system.menu.apiimpl.ApiSysOperationImpl;
import com.carlos.system.news.apiimpl.ApiSysNewsImpl;
import com.carlos.system.region.apiimpl.ApiRegionImpl;
import com.carlos.system.sequence.apiimpl.ApiSequenceImpl;
import com.carlos.system.upload.apiimpl.ApiFileImpl;
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
 * Feign 契约一致性测试：守护 carlos-system 的服务间命名空间（{@code /api/sys/**}）自洽。
 * </p>
 *
 * <p>移植自 {@code WorkflowFeignContractTest}（那边守护 {@code /api/workflow/**}）。
 * 本类存在的直接原因是一次真实缺陷：{@code ApiSysMenu#getMenuById} 曾写成
 * {@code @GetMapping(value = "/")}，而实现侧是裸 {@code @GetMapping} —— 客户端请求
 * {@code /api/sys/menu/}、服务端只注册了 {@code /api/sys/menu}，Spring Framework 6 起
 * 不再做尾斜杠匹配，于是调用必然 500（{@code No static resource}），且<b>没有任何编译期提示</b>。
 * 该方法当时无人调用，缺陷因此潜伏了很久。</p>
 *
 * <p>这里检查的四件事都是实际问题，而非形式检查：</p>
 * <ol>
 *   <li><b>contextId 必须唯一</b>：Spring Cloud 用 contextId 区分同一服务的多个 Feign 客户端，
 *       重复会导致客户端互相覆盖；</li>
 *   <li><b>value 必须是本服务注册名</b>：写错会让调用发到别的服务；</li>
 *   <li><b>Feign 的 path + 方法映射必须与 apiimpl 的 URL 完全一致</b>：不一致时服务间调用
 *       404/500，且不会有任何编译期提示（本类就是为此而写）；</li>
 *   <li><b>实现类不得多挂端点</b>：apiimpl 上任何不在契约接口里的映射，都意味着有接口绕过
 *       {@code /api/**} 的内部定位被单独暴露出去。</li>
 * </ol>
 *
 * @author carlos
 * @since 3.0.0
 */
class SysFeignContractTest {

    /**
     * Feign 接口 → 其实现类（同时也是对服务间暴露的 REST 控制器）
     */
    private static final Map<Class<?>, Class<?>> CLIENTS = new LinkedHashMap<>();

    static {
        CLIENTS.put(ApiDict.class, ApiDictImpl.class);
        CLIENTS.put(ApiFile.class, ApiFileImpl.class);
        CLIENTS.put(ApiRegion.class, ApiRegionImpl.class);
        CLIENTS.put(ApiSequence.class, ApiSequenceImpl.class);
        CLIENTS.put(ApiSysMenu.class, ApiSysMenuImpl.class);
        CLIENTS.put(ApiSysNews.class, ApiSysNewsImpl.class);
        CLIENTS.put(ApiSysOperation.class, ApiSysOperationImpl.class);
        CLIENTS.put(ApiSystemConfig.class, ApiSysConfigImpl.class);
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
                .isEqualTo(ServiceNameConstant.SYSTEM);
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

            assertThat(withLeadingSlash(implBasePath))
                .as("%s 与 %s 的基础路径必须一致", impl.getSimpleName(), client.getSimpleName())
                .isEqualTo(withLeadingSlash(clientBasePath));

            for (Method clientMethod : client.getDeclaredMethods()) {
                Method implMethod = implementationOf(impl, clientMethod);
                RequestMapping clientMapping =
                    AnnotatedElementUtils.findMergedAnnotation(clientMethod, RequestMapping.class);
                assertThat(clientMapping)
                    .as("%s#%s 缺少映射注解", client.getSimpleName(), clientMethod.getName())
                    .isNotNull();

                // 实现方法可以不带映射注解（ApiSysNewsImpl#getById 就是靠接口注解继承工作的），
                // 此时 Spring 用的就是接口上那份映射，故比较对象回落到契约注解
                RequestMapping implOwnMapping =
                    AnnotatedElementUtils.findMergedAnnotation(implMethod, RequestMapping.class);
                RequestMapping effectiveImplMapping = implOwnMapping != null ? implOwnMapping : clientMapping;

                String clientUrl = withLeadingSlash(clientBasePath) + withLeadingSlash(firstPath(clientMapping));
                String implUrl = withLeadingSlash(implBasePath) + withLeadingSlash(firstPath(effectiveImplMapping));

                assertThat(implUrl)
                    .as("%s#%s 的 URL 必须与 Feign 契约一致（尾斜杠也算不一致：Spring 6 起不再做尾斜杠匹配）",
                        impl.getSimpleName(), implMethod.getName())
                    .isEqualTo(clientUrl);

                if (implOwnMapping != null) {
                    assertThat(implOwnMapping.method())
                        .as("%s#%s 的 HTTP 方法必须与 Feign 契约一致",
                            impl.getSimpleName(), implMethod.getName())
                        .isEqualTo(clientMapping.method());
                }
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
                .as("%s 上挂着契约接口之外的端点：服务间接口必须落在 /api/sys/** 的 apiimpl 内，"
                    + "不能借内部定位额外暴露", impl.getSimpleName())
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

    /**
     * 归一化前导斜杠 —— 这是<b>复刻 Feign 的行为</b>，不是「宽容化比较」
     *
     * <p>{@code SpringMvcContract} 在方法级路径不以 {@code /} 开头、且类级 path 不以 {@code /}
     * 结尾时会自行补一个（{@code SpringMvcContract#parseAndValidateMetadata} 的
     * {@code if (!pathValue.startsWith("/") && !data.template().path().endsWith("/"))}）；
     * Spring 侧组合类级与方法级 pattern 时同样会补。所以仓内「{@code item/list} 与
     * {@code /item/list} 并存」只是书写风格差异（{@code ApiDict} 两种都有），
     * 必须在<b>分段</b>时抹平，否则会把大量正常方法误报成缺陷。</p>
     *
     * <p>⚠️ 但<b>绝不能</b>顺带剥掉尾斜杠：Feign 的 {@code RequestTemplate#path()} 是纯字符串拼接
     * （{@code target + uriTemplate}），既不插入也不去除分隔符；Spring Framework 6 起又不再做
     * 尾斜杠匹配。两者叠加，方法级 {@code value = "/"} 会拼出 {@code /api/sys/menu/}，
     * 而服务端只注册了 {@code /api/sys/menu} —— 这正是本类要抓的缺陷
     * （{@code ApiSysMenu#getMenuById} 曾如此，实测 500）。抹掉尾斜杠就等于放跑它。</p>
     *
     * <p>注意归一化必须<b>逐段</b>做（base 与方法路径各归一化一次再相加）。
     * 若先拼接再归一化，{@code api/sys/dict + item/list} 会被当成一整段而漏掉中间的分隔符。</p>
     */
    private static String withLeadingSlash(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

}
