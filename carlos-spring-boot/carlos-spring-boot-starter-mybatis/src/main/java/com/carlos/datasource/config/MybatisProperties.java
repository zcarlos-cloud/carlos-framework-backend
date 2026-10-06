package com.carlos.datasource.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.carlos.datasource.MybatisConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Mybatis 配置属性类
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 */
@Data
@ConfigurationProperties(prefix = "carlos.mybatis")
public class MybatisProperties {

    /**
     * 是否启用 Mybatis 自动配置
     */
    private boolean enabled = true;

    /**
     * 数据库类型
     */
    private DbType dbType = DbType.POSTGRE_SQL;

    /**
     * 分页配置
     */
    private Pagination pagination = new Pagination();

    /**
     * 填充字段配置
     */
    private Fill fill = new Fill();

    /**
     * 拦截器配置
     */
    private Interceptor interceptor = new Interceptor();

    /**
     * 批量操作配置
     */
    private Batch batch = new Batch();

    /**
     * 实体包扫描路径（用于启动期校验 fill 注解：EntityFillValidator 扫描这些包下的
     * 实体，对声明了 fill 但无法自动填充的字段统一告警；不配置则跳过校验）
     */
    private List<String> entityPackages = new ArrayList<>();

    /**
     * 分页配置
     */
    @Data
    public static class Pagination {
        /**
         * 最大单页限制（防止大数据量查询）
         */
        private long maxSize = 500;

        /**
         * 默认页大小
         */
        private long defaultSize = 10;

        /**
         * 是否统计总数
         */
        private boolean searchCount = true;

        /**
         * 是否优化大页码（超过阈值使用流式查询）
         */
        private boolean optimizeLargeOffset = false;

        /**
         * 大页码阈值
         */
        private long largeOffsetThreshold = 100000;

        /**
         * 溢出当前页码是否处理为第一页
         */
        private boolean overflow = false;

        /**
         * 是否优化分页 COUNT 查询（针对 Join 操作）
         * 开启后 COUNT(*) 查询会移除 ORDER BY 和外连接，显著提升复杂查询性能
         */
        private boolean optimizeJoin = true;
    }

    /**
     * 字段填充配置
     */
    @Data
    public static class Fill {
        /**
         * 创建时间字段名
         */
        private String createTimeField = MybatisConstant.DEFAULT_CREATE_TIME_FILED_NAME;

        /**
         * 更新时间字段名
         */
        private String updateTimeField = MybatisConstant.DEFAULT_UPDATE_TIME_FILED_NAME;

        /**
         * 创建人字段名
         */
        private String createByField = MybatisConstant.DEFAULT_CREATE_USER_FILED_NAME;

        /**
         * 更新人字段名
         */
        private String updateByField = MybatisConstant.DEFAULT_UPDATE_USER_FILED_NAME;

        /**
         * 逻辑删除字段名
         */
        private String logicDeleteField = MybatisConstant.DEFAULT_LOGIC_DELETE_FILED_NAME;

        /**
         * 版本号字段名
         */
        private String versionField = MybatisConstant.DEFAULT_VERSION_FILED_NAME;

        /**
         * 租户ID字段名
         */
        private String tenantIdField = MybatisConstant.DEFAULT_TENANT_ID_FILED_NAME;

        /**
         * 是否启用插入填充
         */
        private boolean enableInsertFill = true;

        /**
         * 是否启用更新填充
         */
        private boolean enableUpdateFill = true;

        /**
         * 是否启用注解驱动的通用填充。
         * 开启后，实体上声明了 fill = FieldFill.INSERT/UPDATE/INSERT_UPDATE 的【非通用字段】
         * 也会按策略填充：时间类型填当前时间，命名为 createBy/updateBy（可配置）的字段填当前用户，
         * 无策略字段保持 null 并记录 error 日志（配合 entity-packages 的启动期校验提前暴露）。
         * 关闭则维持仅填充 4 个通用字段的历史行为。
         */
        private boolean annotationFillEnabled = true;
    }

    /**
     * 拦截器配置
     */
    @Data
    public static class Interceptor {
        /**
         * 是否启用分页插件
         */
        private boolean pagination = true;

        /**
         * 是否启用乐观锁
         */
        private boolean optimisticLocker = true;

        /**
         * 是否启用防全表更新删除
         */
        private boolean blockAttack = true;

        /**
         * 是否启用SQL性能分析
         */
        private boolean performance = false;

        /**
         * 慢SQL阈值（毫秒）
         */
        private long slowSqlThreshold = 1000;

        /**
         * 是否打印SQL日志
         */
        private boolean sqlLog = false;

        /**
         * 是否启用租户拦截
         */
        private boolean tenant = false;
    }

    /**
     * 批量操作配置
     */
    @Data
    public static class Batch {
        /**
         * 批量操作批次大小
         */
        private int batchSize = 500;

        /**
         * 是否开启批量插入优化（rewriteBatchedStatements）
         */
        private boolean optimized = true;

        /**
         * 批量操作超时时间（秒）
         */
        private int timeout = 300;

        /**
         * 是否异步执行大批量操作
         */
        private boolean asyncEnabled = false;

        /**
         * 异步执行阈值（超过此数量使用异步）
         */
        private int asyncThreshold = 5000;
    }

    /**
     * 获取 MybatisCommonField 配置
     *
     * @return MybatisCommonField
     */
    public MybatisCommonField toMybatisCommonField() {
        return new MybatisCommonField() {
            @Override
            public String primaryKeyFiledName() {
                return MybatisConstant.DEFAULT_PRIMARY_KEY_FILED_NAME;
            }

            @Override
            public String updateTimeFiledName() {
                return fill.getUpdateTimeField();
            }

            @Override
            public String updateUserFiledName() {
                return fill.getUpdateByField();
            }

            @Override
            public String createTimeFiledName() {
                return fill.getCreateTimeField();
            }

            @Override
            public String createUserFiledName() {
                return fill.getCreateByField();
            }

            @Override
            public String logicDeleteFiledName() {
                return fill.getLogicDeleteField();
            }

            @Override
            public String versionFiledName() {
                return fill.getVersionField();
            }
        };
    }
}
