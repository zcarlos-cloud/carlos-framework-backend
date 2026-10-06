package com.carlos.datasource.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.reflection.MetaObject;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * <p>
 * 字段填充策略处理器（优化版）
 * </p>
 *
 * <p>
 * 使用策略模式替代冗长的 if-else 和 while(true) 代码块
 * 支持配置驱动的字段填充
 * </p>
 *
 * @author carlos
 * @date 2021/2/18 14:25
 */
@Slf4j
public class DefaultMetaObjectHandler implements MetaObjectHandler {

    private final com.carlos.core.interfaces.ApplicationExtend requestExtend;
    private final MybatisCommonField commonField;
    private final MybatisProperties mybatisProperties;

    /**
     * 时间字段填充策略映射
     */
    static final Map<Class<?>, Supplier<?>> TIME_FILLER_MAP = new HashMap<>();

    /**
     * 用户ID字段填充策略映射
     */
    static final Map<Class<?>, Function<Serializable, ?>> USER_ID_FILLER_MAP = new HashMap<>();

    /**
     * 已告警过的无策略 fill 字段（按 类#属性 去重，避免运行期刷日志）
     */
    private final Set<String> reportedUnfillable = ConcurrentHashMap.newKeySet();

    static {
        // 初始化时间填充策略
        TIME_FILLER_MAP.put(Date.class, Date::new);
        TIME_FILLER_MAP.put(LocalDateTime.class, LocalDateTime::now);
        TIME_FILLER_MAP.put(Long.class, System::currentTimeMillis);

        // 初始化用户ID填充策略（类型转换）
        USER_ID_FILLER_MAP.put(Long.class, userId -> userId);
        USER_ID_FILLER_MAP.put(String.class, userId -> String.valueOf(userId));
        USER_ID_FILLER_MAP.put(Integer.class, userId -> Integer.parseInt(String.valueOf(userId)));
        USER_ID_FILLER_MAP.put(BigInteger.class, userId -> new BigInteger(String.valueOf(userId)));
    }

    public DefaultMetaObjectHandler(com.carlos.core.interfaces.ApplicationExtend requestExtend,
                                    MybatisCommonField commonField,
                                    MybatisProperties mybatisProperties) {
        this.requestExtend = requestExtend;
        this.commonField = commonField;
        this.mybatisProperties = mybatisProperties;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        if (!mybatisProperties.getFill().isEnableInsertFill()) {
            return;
        }
        // 填充创建时间
        fillTimeField(metaObject, commonField.createTimeFiledName(), true);

        // 填充更新时间
        fillTimeField(metaObject, commonField.updateTimeFiledName(), true);

        // 填充创建人
        fillUserField(metaObject, commonField.createUserFiledName(), true);

        // 填充更新人（insert 时与创建人一致）
        fillUserField(metaObject, commonField.updateUserFiledName(), true);

        // 注解驱动的通用填充：兑现实体上非通用字段的 fill = FieldFill.INSERT 契约
        if (mybatisProperties.getFill().isAnnotationFillEnabled()) {
            fillAnnotatedFields(metaObject, true);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        if (!mybatisProperties.getFill().isEnableUpdateFill()) {
            return;
        }
        // 填充更新时间
        fillTimeField(metaObject, commonField.updateTimeFiledName(), false);

        // 填充更新人
        fillUserField(metaObject, commonField.updateUserFiledName(), false);

        // 注解驱动的通用填充：兑现实体上非通用字段的 fill = FieldFill.UPDATE/INSERT_UPDATE 契约
        if (mybatisProperties.getFill().isAnnotationFillEnabled()) {
            fillAnnotatedFields(metaObject, false);
        }
    }

    /**
     * 填充时间字段
     *
     * @param metaObject 元对象
     * @param fieldName  字段名
     * @param isInsert   是否插入操作
     */
    private void fillTimeField(MetaObject metaObject, String fieldName, boolean isInsert) {
        if (StringUtils.isBlank(fieldName) || !metaObject.hasSetter(fieldName)) {
            return;
        }

        Object currentValue = metaObject.getValue(fieldName);
        if (currentValue != null) {
            // 已有值，不覆盖
            return;
        }

        Class<?> fieldType = metaObject.getGetterType(fieldName);
        if (!isTimeFillableType(fieldType)) {
            log.warn("不支持的时间字段类型: {}.{} = {}",
                    metaObject.getOriginalObject().getClass().getSimpleName(),
                    fieldName, fieldType.getSimpleName());
            return;
        }
        fillTimeValue(metaObject, fieldName, fieldType, isInsert);
    }

    /**
     * 按类型填充时间值（不覆盖已有值，调用前需自行判空）
     *
     * @param metaObject 元对象
     * @param fieldName  字段名
     * @param fieldType  字段类型
     * @param isInsert   是否插入操作
     */
    @SuppressWarnings("unchecked")
    private void fillTimeValue(MetaObject metaObject, String fieldName, Class<?> fieldType, boolean isInsert) {
        Supplier<?> filler = TIME_FILLER_MAP.get(fieldType);
        if (filler == null) {
            log.warn("不支持的时间字段类型: {}.{} = {}",
                    metaObject.getOriginalObject().getClass().getSimpleName(),
                    fieldName, fieldType.getSimpleName());
            return;
        }
        Object value = filler.get();
        if (isInsert) {
            strictInsertFill(metaObject, fieldName, (Class<Object>) fieldType, value);
        } else {
            strictUpdateFill(metaObject, fieldName, (Class<Object>) fieldType, value);
        }
        log.debug("填充时间字段: {} = {}", fieldName, value);
    }

    /**
     * 注解驱动的通用填充：扫描实体 TableInfo 中声明了 fill 的字段，按策略填充 null 值。
     * 这是 MyBatis-Plus 标准契约的兑现——框架不再只认 4 个通用字段，
     * 无策略字段保持 null 并按字段维度告警一次（运行时兜底，启动期校验见 EntityFillValidator）。
     *
     * @param metaObject 元对象
     * @param isInsert   是否插入操作
     */
    private void fillAnnotatedFields(MetaObject metaObject, boolean isInsert) {
        Object original = metaObject.getOriginalObject();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(original.getClass());
        if (tableInfo == null) {
            return;
        }
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            FieldFill fieldFill = fieldInfo.getFieldFill();
            if (fieldFill == null || fieldFill == FieldFill.DEFAULT) {
                continue;
            }
            // INSERT 字段只在插入时填充；UPDATE 字段只在更新时填充；INSERT_UPDATE 两者都填充
            if (isInsert ? fieldFill == FieldFill.UPDATE : fieldFill == FieldFill.INSERT) {
                continue;
            }
            String property = fieldInfo.getProperty();
            if (!metaObject.hasSetter(property) || metaObject.getValue(property) != null) {
                continue;
            }
            Class<?> propertyType = fieldInfo.getPropertyType();
            switch (FillFieldStrategy.resolve(property, propertyType, commonField)) {
                case TIME -> fillTimeValue(metaObject, property, propertyType, isInsert);
                case USER -> fillUserValue(metaObject, property, propertyType, isInsert);
                default -> reportUnfillable(original.getClass(), property, propertyType);
            }
        }
    }

    /**
     * 无策略 fill 字段告警（按 类#属性 维度去重，error 级——WARN 日志在本框架默认不可见）
     *
     * @param entityClass  实体类
     * @param property     属性名
     * @param propertyType 属性类型
     */
    private void reportUnfillable(Class<?> entityClass, String property, Class<?> propertyType) {
        String key = entityClass.getName() + '#' + property;
        if (reportedUnfillable.add(key)) {
            log.error("实体字段声明了 fill 注解但无填充策略（保持 null，插入非空约束时将报错）: {}.{} : {}"
                            + " —— 请业务侧显式赋值，或改用时间类型 / 命名为 {}、{} 的字段",
                    entityClass.getSimpleName(), property, propertyType.getSimpleName(),
                    commonField.createUserFiledName(), commonField.updateUserFiledName());
        }
    }

    /**
     * 判断类型是否可填充时间值
     *
     * @param type 字段类型
     * @return 是否时间可填充类型
     */
    static boolean isTimeFillableType(Class<?> type) {
        return TIME_FILLER_MAP.containsKey(type);
    }

    /**
     * 填充用户字段
     *
     * @param metaObject 元对象
     * @param fieldName  字段名
     * @param isInsert   是否插入操作
     */
    private void fillUserField(MetaObject metaObject, String fieldName, boolean isInsert) {
        if (StringUtils.isBlank(fieldName) || !metaObject.hasSetter(fieldName)) {
            return;
        }

        Object currentValue = metaObject.getValue(fieldName);
        if (currentValue != null) {
            // 已有值，不覆盖
            return;
        }

        fillUserValue(metaObject, fieldName, metaObject.getGetterType(fieldName), isInsert);
    }

    /**
     * 填充当前用户 ID 值（不覆盖已有值，调用前需自行判空）
     *
     * @param metaObject 元对象
     * @param fieldName  字段名
     * @param fieldType  字段类型
     * @param isInsert   是否插入操作
     */
    @SuppressWarnings("unchecked")
    private void fillUserValue(MetaObject metaObject, String fieldName, Class<?> fieldType, boolean isInsert) {
        Serializable userId = getUserId();
        if (userId == null) {
            log.debug("无法获取当前用户ID，跳过填充字段: {}", fieldName);
            return;
        }

        Function<Serializable, ?> converter = USER_ID_FILLER_MAP.get(fieldType);
        if (converter == null) {
            log.warn("不支持的用户ID字段类型: {}.{} = {}",
                    metaObject.getOriginalObject().getClass().getSimpleName(),
                    fieldName, fieldType.getSimpleName());
            return;
        }
        try {
            Object value = converter.apply(userId);
            if (isInsert) {
                strictInsertFill(metaObject, fieldName, (Class<Object>) fieldType, value);
            } else {
                strictUpdateFill(metaObject, fieldName, (Class<Object>) fieldType, value);
            }
            log.debug("填充用户字段: {} = {}", fieldName, value);
        } catch (Exception e) {
            log.warn("用户ID类型转换失败: {} -> {}", userId, fieldType.getSimpleName(), e);
        }
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    private Serializable getUserId() {
        if (requestExtend == null) {
            return null;
        }
        try {
            return requestExtend.getUserId();
        } catch (Exception e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 注册自定义的时间字段填充策略
     *
     * @param type   字段类型
     * @param filler 填充函数
     */
    public static void registerTimeFiller(Class<?> type, Supplier<?> filler) {
        TIME_FILLER_MAP.put(type, filler);
    }

    /**
     * 注册自定义的用户ID字段填充策略
     *
     * @param type      字段类型
     * @param converter 转换函数
     */
    public static void registerUserIdConverter(Class<?> type, Function<Serializable, ?> converter) {
        USER_ID_FILLER_MAP.put(type, converter);
    }
}
