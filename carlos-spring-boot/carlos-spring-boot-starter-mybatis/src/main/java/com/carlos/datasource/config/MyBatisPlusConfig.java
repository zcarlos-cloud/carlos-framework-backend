package com.carlos.datasource.config;


import com.baomidou.mybatisplus.autoconfigure.IdentifierGeneratorAutoConfiguration;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.datascope.DataScopeHandler;
import com.carlos.datasource.health.MybatisHealthIndicator;
import com.carlos.datasource.interceptor.PerformanceInterceptor;
import com.carlos.datasource.scope.CustomDataPermissionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * <p>
 * MyBatis Plus 自动配置类（增强版）
 * </p>
 *
 * @author carlos
 * @date 2020/4/9 16:26
 */
@Slf4j
@AutoConfiguration(before = {IdentifierGeneratorAutoConfiguration.class})
@EnableTransactionManagement
@EnableConfigurationProperties(MybatisProperties.class)
@RequiredArgsConstructor
public class MyBatisPlusConfig {

    private final MybatisProperties mybatisProperties;

    /**
     * 自定义 ID 生成器
     */
    @Bean
    @ConditionalOnMissingBean
    public IdentifierGenerator customizeIdGenerator() {
        return new CustomizeIdGenerator();
    }

    /**
     * Mybatis 通用字段配置
     *
     * @return MybatisCommonField
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisCommonField commonFiled() {
        // 优先使用配置中的字段名
        return mybatisProperties.toMybatisCommonField();
    }

    /**
     * 元对象字段填充处理器
     *
     * @param requestExtend ApplicationExtend
     * @param commonField   MybatisCommonField
     * @return MetaObjectHandler
     */
    @Bean
    @ConditionalOnMissingBean
    public MetaObjectHandler defaultMetaObjectHandler(
        @Nullable @Lazy ApplicationExtend requestExtend,
        MybatisCommonField commonField) {
        return new DefaultMetaObjectHandler(requestExtend, commonField, mybatisProperties);
    }

    /**
     * 实体 fill 注解启动期校验器。
     * 扫描 carlos.mybatis.entity-packages 下的实体，对声明了 fill 但无法自动填充的字段
     * 启动时统一 error 告警（不阻断）。entity-packages 未配置时不做任何事。
     *
     * @param commonField MybatisCommonField
     * @return EntityFillValidator
     */
    @Bean
    @ConditionalOnMissingBean
    public EntityFillValidator entityFillValidator(MybatisCommonField commonField) {
        return new EntityFillValidator(mybatisProperties, commonField);
    }

    /**
     * Mybatis Plus 拦截器配置
     *
     * @param dataPermissionHandler 数据权限处理器（可选）
     * @param tenantLineHandler     租户处理器（可选）
     * @return MybatisPlusInterceptor
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(
        @Nullable DataPermissionHandler dataPermissionHandler,
        @Nullable TenantLineHandler tenantLineHandler) {

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        MybatisProperties.Interceptor interceptorConfig = mybatisProperties.getInterceptor();

        // 1. 租户拦截器（最先执行）
        if (interceptorConfig.isTenant() && tenantLineHandler != null) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
            log.info("租户拦截器已启用");
        }

        // 2. 数据权限拦截器
        if (dataPermissionHandler != null) {
            interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataPermissionHandler));
            log.info("数据权限拦截器已启用");
        }

        // 3. 分页拦截器
        if (interceptorConfig.isPagination()) {
            PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(
                mybatisProperties.getDbType());

            MybatisProperties.Pagination paginationConfig = mybatisProperties.getPagination();
            paginationInterceptor.setMaxLimit(paginationConfig.getMaxSize());
            paginationInterceptor.setOverflow(paginationConfig.isOverflow());
            paginationInterceptor.setOptimizeJoin(paginationConfig.isOptimizeJoin());

            interceptor.addInnerInterceptor(paginationInterceptor);
            log.info("分页拦截器已启用，最大限制: {}", paginationConfig.getMaxSize());
        }

        // 4. 乐观锁拦截器
        if (interceptorConfig.isOptimisticLocker()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
            log.info("乐观锁拦截器已启用");
        }

        // 5. 防全表更新删除拦截器
        if (interceptorConfig.isBlockAttack()) {
            interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
            log.info("防全表更新删除拦截器已启用");
        }

        // 6. SQL 性能分析拦截器（如果启用）
        if (interceptorConfig.isPerformance()) {
            PerformanceInterceptor performanceInterceptor = new PerformanceInterceptor();
            performanceInterceptor.setSlowSqlThreshold(interceptorConfig.getSlowSqlThreshold());
            performanceInterceptor.setPrintAllSql(interceptorConfig.isSqlLog());
            interceptor.addInnerInterceptor(performanceInterceptor);
            log.info("SQL性能分析拦截器已启用，慢SQL阈值: {}ms", interceptorConfig.getSlowSqlThreshold());
        }

        return interceptor;
    }

    /**
     * 数据权限配置（条件装配）
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(DataScopeHandler.class)
    @RequiredArgsConstructor
    static class DataPermissionConfig {

        private final DataScopeHandler dataScopeHandler;

        @Bean
        @ConditionalOnMissingBean
        public CustomDataPermissionHandler dataPermissionHandler() {
            return new CustomDataPermissionHandler(dataScopeHandler);
        }
    }

    /**
     * 初始化日志
     */
    @Bean
    public String mybatisConfigLog() {
        log.info("MyBatis Plus 配置加载完成，数据库类型: {}", mybatisProperties.getDbType());
        return "MyBatis Plus Configured";
    }

    /**
     * 数据源健康检查指示器
     */
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    public MybatisHealthIndicator mybatisHealthIndicator(DataSource dataSource) {
        return new MybatisHealthIndicator(dataSource);
    }
}
