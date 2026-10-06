package com.carlos.datasource.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 实体 fill 注解启动期校验器
 * </p>
 *
 * <p>
 * 扫描 {@code carlos.mybatis.entity-packages} 配置的包下的实体（{@code @TableName} 标注），
 * 凡是声明了 {@code fill = FieldFill.INSERT/UPDATE/INSERT_UPDATE} 却无法自动填充的字段
 * （非时间类型、非 createBy/updateBy 命名），在启动时统一 error 告警。
 * 告警不阻断启动——运行时插入若违反非空约束仍会报错，该校验只是把问题提前暴露。
 * 判定口径与 {@link DefaultMetaObjectHandler} 一致（共用 {@link FillFieldStrategy}）。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
public class EntityFillValidator implements SmartInitializingSingleton {

    private final MybatisProperties mybatisProperties;
    private final MybatisCommonField commonField;

    public EntityFillValidator(MybatisProperties mybatisProperties, MybatisCommonField commonField) {
        this.mybatisProperties = mybatisProperties;
        this.commonField = commonField;
    }

    @Override
    public void afterSingletonsInstantiated() {
        List<String> packages = mybatisProperties.getEntityPackages();
        if (packages == null || packages.isEmpty()) {
            return;
        }
        // 注解通用填充被关闭时，fill 注解本就不参与填充（维持历史行为），无需校验
        if (!mybatisProperties.getFill().isAnnotationFillEnabled()) {
            return;
        }
        List<String> violations = validate(packages);
        if (violations.isEmpty()) {
            return;
        }
        // 告警不阻断：error 级——WARN 日志在本框架默认不可见（只落 debug.log）
        log.error("实体 fill 注解校验发现 {} 处无法自动填充的字段（保持 null，插入非空约束时将报错）。"
                        + "请业务侧显式赋值，或改用时间类型 / 命名为 {}、{} 的字段:\n{}",
                violations.size(), commonField.createUserFiledName(), commonField.updateUserFiledName(),
                String.join("\n", violations));
    }

    /**
     * 校验指定包下的实体，返回无法自动填充的 fill 字段清单
     *
     * @param packages 实体包路径
     * @return 违规描述列表（空表示全部通过）
     */
    List<String> validate(List<String> packages) {
        List<String> violations = new ArrayList<>();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(TableName.class));
        for (String pkg : packages) {
            if (!StringUtils.hasText(pkg)) {
                continue;
            }
            for (BeanDefinition beanDefinition : scanner.findCandidateComponents(pkg)) {
                Class<?> entityClass = resolveClass(beanDefinition.getBeanClassName());
                if (entityClass == null) {
                    continue;
                }
                collectViolations(entityClass, violations);
            }
        }
        return violations;
    }

    private Class<?> resolveClass(String className) {
        try {
            return ClassUtils.forName(className, getClass().getClassLoader());
        } catch (Throwable e) {
            log.warn("实体 fill 校验：无法加载类 {}", className, e);
            return null;
        }
    }

    private void collectViolations(Class<?> entityClass, List<String> violations) {
        for (Class<?> current = entityClass;
             current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                // static 字段不参与持久化（与 TableInfoHelper 口径一致），跳过
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                // 主键不参与 fill 填充（由 IdentifierGenerator 负责），跳过
                if (field.isAnnotationPresent(TableId.class)) {
                    continue;
                }
                TableField tableField = field.getAnnotation(TableField.class);
                if (tableField == null || tableField.fill() == FieldFill.DEFAULT) {
                    continue;
                }
                // exist = false 的瞬态字段不参与 SQL（运行期 TableInfo.getFieldList() 不含），与运行期口径对齐
                if (!tableField.exist()) {
                    continue;
                }
                if (FillFieldStrategy.resolve(field.getName(), field.getType(), commonField) == FillStrategy.UNKNOWN) {
                    violations.add(entityClass.getSimpleName() + "." + field.getName()
                            + "(" + field.getType().getSimpleName() + ", fill=" + tableField.fill() + ")");
                }
            }
        }
    }
}
