package com.carlos.cloud.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * FallbackFactory 自动注册器
 * </p>
 *
 * <p>
 * 自动扫描并注册所有实现了 {@link FallbackFactory} 接口的类为 Spring Bean。
 * 扫描的包路径通过配置属性 {@code carlos.feign.fallback.scan-packages} 指定，
 * 默认扫描 {@code com.carlos} 包及其子包。
 * </p>
 *
 * <p>
 * 使用示例：
 * <pre>
 * # 自定义扫描包路径
 * carlos:
 *   feign:
 *     fallback:
 *       scan-packages:
 *         - com.carlos.system.fallback
 *         - com.carlos.org.fallback
 * </pre>
 * </p>
 *
 * <p>
 * 注意：
 * <ul>
 *   <li>如果 FallbackFactory 已经通过 {@code @Bean} 或 {@code @Component} 注册，则不会重复注册</li>
 *   <li>抽象类不会被注册</li>
 *   <li>接口不会被注册</li>
 * </ul>
 * </p>
 *
 * @author carlos
 * @date 2026-03-24
 */
@Slf4j
@ConditionalOnClass(FallbackFactory.class)
@ConditionalOnProperty(prefix = "carlos.feign.fallback", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FallbackFactoryAutoRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    /**
     * 默认扫描包路径
     */
    private static final String DEFAULT_SCAN_PACKAGE = "com.carlos";

    /**
     * 配置属性前缀
     */
    private static final String CONFIG_PREFIX = "carlos.feign.fallback.scan-packages";

    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<String> scanPackages = getScanPackages();
        if (CollectionUtils.isEmpty(scanPackages)) {
            log.debug("未配置 FallbackFactory 扫描包路径，跳过自动注册");
            return;
        }

        log.info("开始自动扫描 FallbackFactory，扫描包路径: {}", scanPackages);

        // 创建类路径扫描器
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(FallbackFactory.class));
        if (resourceLoader != null) {
            scanner.setResourceLoader(resourceLoader);
        }

        Set<String> registeredBeans = new HashSet<>();
        int count = 0;

        for (String basePackage : scanPackages) {
            if (!StringUtils.hasText(basePackage)) {
                continue;
            }

            try {
                Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
                for (BeanDefinition candidate : candidates) {
                    String className = candidate.getBeanClassName();
                    if (!StringUtils.hasText(className)) {
                        continue;
                    }

                    // 检查是否已经注册（通过 beanName 或 className）
                    String beanName = generateBeanName(className);
                    if (registry.containsBeanDefinition(beanName) || isBeanAlreadyRegistered(registry, className)) {
                        log.debug("FallbackFactory [{}] 已存在，跳过自动注册", className);
                        continue;
                    }

                    // 注册 Bean
                    registerFallbackFactory(registry, className, beanName);
                    registeredBeans.add(className);
                    count++;
                    log.debug("自动注册 FallbackFactory: [{}] as bean [{}]", className, beanName);
                }
            } catch (Exception e) {
                log.warn("扫描包 [{}] 时发生错误: {}", basePackage, e.getMessage());
            }
        }

        if (count > 0) {
            log.info("成功自动注册 [{}] 个 FallbackFactory: {}", count, registeredBeans);
        } else {
            log.debug("未找到需要自动注册的 FallbackFactory");
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 不需要处理
    }

    /**
     * 获取扫描包路径
     */
    private Set<String> getScanPackages() {
        Set<String> packages = new HashSet<>();

        // 从配置中读取
        String packagesStr = environment.getProperty(CONFIG_PREFIX);
        if (StringUtils.hasText(packagesStr)) {
            // 支持逗号分隔或数组形式
            String[] packageArray = packagesStr.split(",");
            for (String pkg : packageArray) {
                if (StringUtils.hasText(pkg.trim())) {
                    packages.add(pkg.trim());
                }
            }
        } else {
            // 尝试读取数组形式的配置
            int index = 0;
            while (true) {
                String pkg = environment.getProperty(CONFIG_PREFIX + "[" + index + "]");
                if (!StringUtils.hasText(pkg)) {
                    break;
                }
                packages.add(pkg.trim());
                index++;
            }
        }

        // 如果没有配置，使用默认值
        if (packages.isEmpty()) {
            packages.add(DEFAULT_SCAN_PACKAGE);
        }

        return packages;
    }

    /**
     * 生成 Bean 名称
     */
    private String generateBeanName(String className) {
        // 获取简单类名（不含包名）
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        // 首字母小写
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    /**
     * 检查 Bean 是否已注册
     */
    private boolean isBeanAlreadyRegistered(BeanDefinitionRegistry registry, String className) {
        String[] beanNames = registry.getBeanDefinitionNames();
        for (String existingBeanName : beanNames) {
            BeanDefinition existingDef = registry.getBeanDefinition(existingBeanName);
            if (className.equals(existingDef.getBeanClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 注册 FallbackFactory Bean
     */
    private void registerFallbackFactory(BeanDefinitionRegistry registry, String className, String beanName) {
        try {
            Class<?> clazz = ClassUtils.forName(className, getClass().getClassLoader());

            // 跳过抽象类和接口
            if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                log.debug("跳过抽象类或接口: {}", className);
                return;
            }

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
            builder.setRole(BeanDefinition.ROLE_APPLICATION);

            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
        } catch (ClassNotFoundException e) {
            log.warn("无法加载类 [{}]: {}", className, e.getMessage());
        }
    }
}
