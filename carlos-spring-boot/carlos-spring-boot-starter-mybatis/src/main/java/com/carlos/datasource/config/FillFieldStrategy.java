package com.carlos.datasource.config;

/**
 * <p>
 * fill 注解字段的填充策略判定
 * </p>
 *
 * <p>
 * DefaultMetaObjectHandler（运行期填充）与 EntityFillValidator（启动期校验）共用，
 * 保证「哪些字段能自动填充」的口径两处一致。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
final class FillFieldStrategy {

    private FillFieldStrategy() {
    }

    /**
     * 判定字段的填充策略
     *
     * <p>
     * 注意：Long 一律按时间戳处理（填 currentTimeMillis），不会当作用户 ID。
     * 需要填充用户 ID 的字段必须命名为 createBy/updateBy（可配置），
     * 给 Long 型业务字段（如 ownerId）加 fill 注解会被填成时间戳而非用户 ID。
     * </p>
     *
     * @param property    实体属性名
     * @param fieldType   字段类型
     * @param commonField 通用字段命名配置
     * @return 填充策略
     */
    static FillStrategy resolve(String property, Class<?> fieldType, MybatisCommonField commonField) {
        if (property.equals(commonField.createUserFiledName()) || property.equals(commonField.updateUserFiledName())) {
            return FillStrategy.USER;
        }
        if (DefaultMetaObjectHandler.isTimeFillableType(fieldType)) {
            return FillStrategy.TIME;
        }
        return FillStrategy.UNKNOWN;
    }
}
