package com.carlos.mongodb.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionField;
import com.mongoplus.annotation.collection.CollectionName;
import com.mongoplus.enums.FieldFill;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志文档实体类
 * MongoDB 集合: log_document
 *
 * <p><strong>mongo-plus 注解：</strong></p>
 * <ul>
 *   <li>{@code @CollectionName("log_document")} — 指定集合名</li>
 *   <li>{@code @ID} — 标记主键字段</li>
 *   <li>{@code @CollectionField(fill = FieldFill.INSERT)} — 插入时自动填充</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026/3/15
 */
@Data
@CollectionName("log_document")
public class LogDocument {

    @ID
    private String id;

    /**
     * 日志级别
     */
    private String level;

    /**
     * 日志模块
     */
    private String module;

    /**
     * 操作内容
     */
    private String content;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 请求IP
     */
    private String ip;

    /**
     * 请求路径
     */
    private String requestUrl;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 执行时长(毫秒)
     */
    private Long executionTime;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间（插入时自动填充）
     */
    @CollectionField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（插入+更新时自动填充）
     */
    @CollectionField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
