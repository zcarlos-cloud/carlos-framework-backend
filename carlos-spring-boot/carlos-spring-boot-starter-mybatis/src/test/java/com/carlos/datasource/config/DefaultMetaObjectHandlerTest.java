package com.carlos.datasource.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.carlos.core.interfaces.ApplicationExtend;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DefaultMetaObjectHandler 注解驱动通用填充 单元测试
 *
 * @author carlos
 * @since 3.0.0
 */
class DefaultMetaObjectHandlerTest {

    @Data
    @TableName("test_fill_entity")
    static class TestFillEntity {

        @TableId(type = IdType.ASSIGN_ID)
        private Long id;

        @TableField(value = "create_time", fill = FieldFill.INSERT)
        private LocalDateTime createTime;

        @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;

        /**
         * 非通用字段名 + fill 注解：注解通用填充应兑现（本次修复的场景）
         */
        @TableField(value = "read_time", fill = FieldFill.INSERT)
        private LocalDateTime readTime;

        /**
         * 通用用户字段名 + fill 注解：注解通用填充应填当前用户 ID
         */
        @TableField(value = "create_by", fill = FieldFill.INSERT)
        private Long createBy;

        /**
         * 无填充策略的类型：保持 null 并告警
         */
        @TableField(value = "enabled", fill = FieldFill.INSERT)
        private Boolean enabled;

        /**
         * 未声明 fill：不参与填充
         */
        @TableField("plain_value")
        private String plainValue;
    }

    /**
     * 父类持有 fill 字段：验证 TableInfo 合并父类字段后注解填充仍生效
     */
    @Data
    static class TestFillParent {

        @TableField(value = "create_time", fill = FieldFill.INSERT)
        private LocalDateTime createTime;
    }

    @Data
    @TableName("test_fill_child")
    @EqualsAndHashCode(callSuper = true)
    static class TestFillChild extends TestFillParent {

        @TableId(type = IdType.ASSIGN_ID)
        private Long id;
    }

    private MybatisProperties properties;
    private DefaultMetaObjectHandler handler;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, TestFillEntity.class);
        TableInfoHelper.initTableInfo(assistant, TestFillChild.class);
    }

    @BeforeEach
    void setUp() {
        properties = new MybatisProperties();
        handler = new DefaultMetaObjectHandler(null, properties.toMybatisCommonField(), properties);
    }

    @Test
    @DisplayName("insert 时应填充通用字段与注解声明的非通用时间字段，无策略字段保持 null")
    void should_fill_annotated_fields_on_insert() {
        TestFillEntity entity = new TestFillEntity();
        handler.insertFill(SystemMetaObject.forObject(entity));

        assertNotNull(entity.getCreateTime(), "createTime 应填充");
        assertNotNull(entity.getUpdateTime(), "updateTime（INSERT_UPDATE）在 insert 时应填充");
        assertNotNull(entity.getReadTime(), "非通用字段名 readTime（fill=INSERT）应由注解通用填充兑现");
        assertNull(entity.getEnabled(), "Boolean 无策略字段应保持 null");
        assertNull(entity.getPlainValue(), "未声明 fill 的字段不应填充");
    }

    @Test
    @DisplayName("update 时应填充 INSERT_UPDATE 字段，INSERT 专属字段不重复填充")
    void should_fill_update_fields_on_update() {
        TestFillEntity entity = new TestFillEntity();
        MetaObject metaObject = SystemMetaObject.forObject(entity);
        handler.insertFill(metaObject);

        // 模拟：insert 后清空，update 阶段 INSERT 字段不应被回填
        entity.setUpdateTime(null);
        entity.setReadTime(null);
        handler.updateFill(metaObject);

        assertNotNull(entity.getUpdateTime(), "updateTime（INSERT_UPDATE）在 update 时应填充");
        assertNull(entity.getReadTime(), "readTime（INSERT 专属）在 update 时不应填充");
    }

    @Test
    @DisplayName("annotationFillEnabled=false 时维持历史行为：非通用字段不填充")
    void should_skip_annotation_fill_when_disabled() {
        properties.getFill().setAnnotationFillEnabled(false);
        TestFillEntity entity = new TestFillEntity();
        handler.insertFill(SystemMetaObject.forObject(entity));

        assertNotNull(entity.getCreateTime(), "通用字段仍填充");
        assertNull(entity.getReadTime(), "关闭注解填充后 readTime 不填充");
    }

    @Test
    @DisplayName("已有值不覆盖")
    void should_not_overwrite_existing_value() {
        LocalDateTime preset = LocalDateTime.of(2020, 1, 1, 0, 0);
        TestFillEntity entity = new TestFillEntity();
        entity.setReadTime(preset);
        handler.insertFill(SystemMetaObject.forObject(entity));

        assertEquals(preset, entity.getReadTime(), "已有值不应被覆盖");
    }

    @Test
    @DisplayName("createBy 命中用户字段命名时注解填充应填当前用户 ID")
    void should_fill_user_id_for_create_by_field() {
        ApplicationExtend extend = mock(ApplicationExtend.class);
        when(extend.getUserId()).thenReturn(42L);
        DefaultMetaObjectHandler userHandler =
                new DefaultMetaObjectHandler(extend, properties.toMybatisCommonField(), properties);

        TestFillEntity entity = new TestFillEntity();
        userHandler.insertFill(SystemMetaObject.forObject(entity));

        assertEquals(42L, entity.getCreateBy(), "createBy 应填当前用户 ID");
        assertNotNull(entity.getReadTime(), "时间字段仍应由注解填充兑现");
    }

    @Test
    @DisplayName("fill 字段声明在父类时，子实体插入也应填充")
    void should_fill_inherited_fill_fields() {
        TestFillChild entity = new TestFillChild();
        handler.insertFill(SystemMetaObject.forObject(entity));

        assertNotNull(entity.getCreateTime(), "父类声明的 createTime 应在子实体插入时填充");
    }
}
