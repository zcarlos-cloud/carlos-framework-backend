package com.carlos.mongodb.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * MongoDB 配置属性类
 * </p>
 *
 * <p>
 * 通过 {@code carlos.mongodb.*} 命名空间配置 MongoDB 模块行为，
 * 包括字段自动填充、逻辑删除、乐观锁等增强功能。
 * </p>
 *
 * <p><strong>mongo-plus 集成：</strong></p>
 * <ul>
 *   <li>字段自动填充由 mongo-plus 的 {@code @CollectionField(fill = FieldFill.INSERT)} 注解驱动</li>
 *   <li>逻辑删除由 mongo-plus 内置的 {@code @LogicDelete} 注解支持</li>
 *   <li>乐观锁由 mongo-plus 内置的 {@code @Version} 注解支持</li>
 *   <li>以下配置仅用于定义字段名，实际功能由 mongo-plus 自动装配</li>
 * </ul>
 *
 * @author carlos
 * @date 2024/01/15
 */
@Data
@ConfigurationProperties(prefix = "carlos.mongodb")
public class MongoProperties {

    /**
     * 是否启用 MongoDB 模块
     */
    private boolean enabled = true;

    // ==================== 字段自动填充配置 ====================

    /**
     * 是否启用字段自动填充（createTime, updateTime, createBy, updateBy）
     */
    private boolean fieldFillEnabled = true;

    /**
     * 创建时间字段名
     */
    private String createTimeField = "createTime";

    /**
     * 更新时间字段名
     */
    private String updateTimeField = "updateTime";

    /**
     * 创建人字段名
     */
    private String createByField = "createBy";

    /**
     * 更新人字段名
     */
    private String updateByField = "updateBy";

    // ==================== 逻辑删除配置 ====================

    /**
     * 是否启用逻辑删除
     */
    private boolean logicDeleteEnabled = false;

    /**
     * 逻辑删除字段名
     */
    private String deletedField = "deleted";

    // ==================== 乐观锁配置 ====================

    /**
     * 是否启用乐观锁
     */
    private boolean optimisticLockEnabled = false;

    /**
     * 乐观锁版本号字段名
     */
    private String versionField = "version";
}
