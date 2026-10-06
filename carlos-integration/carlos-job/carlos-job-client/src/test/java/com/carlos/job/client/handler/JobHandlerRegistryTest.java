package com.carlos.job.client.handler;

import com.carlos.core.exception.ComponentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * 任务处理器注册表单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
class JobHandlerRegistryTest {

    private JobHandlerRegistry registry;

    private JobHandler demoHandler;

    @BeforeEach
    void setUp() {
        demoHandler = params -> JobResult.ok();
        Map<String, JobHandler> beans = new HashMap<>();
        beans.put("demoJobHandler", demoHandler);
        registry = new JobHandlerRegistry(beans);
        registry.afterSingletonsInstantiated();
    }

    @Test
    @DisplayName("应该按Bean名称注册处理器当未自定义name时")
    void should_register_by_bean_name_when_no_custom_name() {
        assertSame(demoHandler, registry.getHandler("demoJobHandler"));
        assertTrue(registry.handlerNames().contains("demoJobHandler"));
    }

    @Test
    @DisplayName("应该使用自定义名称当handler重写name方法时")
    void should_use_custom_name_when_handler_overrides_name() {
        JobHandler customHandler = new JobHandler() {
            @Override
            public String name() {
                return "customJob";
            }

            @Override
            public JobResult execute(String params) {
                return JobResult.ok();
            }
        };
        Map<String, JobHandler> beans = new HashMap<>();
        beans.put("whateverBeanName", customHandler);
        JobHandlerRegistry customRegistry = new JobHandlerRegistry(beans);
        customRegistry.afterSingletonsInstantiated();

        assertSame(customHandler, customRegistry.getHandler("customJob"));
        assertNull(customRegistry.getHandler("whateverBeanName"));
    }

    @Test
    @DisplayName("应该保留先注册的处理器当名称冲突时")
    void should_keep_first_handler_when_name_conflicts() {
        JobHandler another = new JobHandler() {
            @Override
            public String name() {
                return "demoJobHandler";
            }

            @Override
            public JobResult execute(String params) {
                return JobResult.ok();
            }
        };
        // LinkedHashMap 固定迭代顺序，保证 demoHandler 先注册
        Map<String, JobHandler> beans = new LinkedHashMap<>();
        beans.put("demoJobHandler", demoHandler);
        beans.put("anotherHandler", another);
        JobHandlerRegistry conflictRegistry = new JobHandlerRegistry(beans);
        conflictRegistry.afterSingletonsInstantiated();

        assertSame(demoHandler, conflictRegistry.getHandler("demoJobHandler"));
        assertEquals(1, conflictRegistry.handlerNames().size());
    }

    @Test
    @DisplayName("应该返回null当处理器名称不存在或为空时")
    void should_return_null_when_handler_name_not_found_or_blank() {
        assertNull(registry.getHandler("notExists"));
        assertNull(registry.getHandler(null));
        assertNull(registry.getHandler(""));
    }

    @Test
    @DisplayName("应该启动失败当接口处理器撞上注解已占用的名称时")
    void should_fail_fast_when_bean_name_collides_with_annotated_name() {
        JobHandler annotated = params -> JobResult.ok();
        Map<String, JobHandler> beans = new HashMap<>();
        beans.put("collidedBean", demoHandler);
        JobHandlerRegistry conflictRegistry = new JobHandlerRegistry(beans, Map.of("collidedBean", annotated));

        assertThrows(ComponentException.class, conflictRegistry::afterSingletonsInstantiated);
    }

    @Test
    @DisplayName("应该抛出异常当严格登记的名称已存在时")
    void should_throw_when_register_duplicate_name() {
        JobHandler another = params -> JobResult.ok();

        assertThrows(ComponentException.class, () -> registry.register("demoJobHandler", another));
        assertSame(demoHandler, registry.getHandler("demoJobHandler"));
    }

}
