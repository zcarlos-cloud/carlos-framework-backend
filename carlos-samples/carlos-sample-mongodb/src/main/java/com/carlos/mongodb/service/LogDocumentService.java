package com.carlos.mongodb.service;

import com.carlos.mongodb.base.BaseService;
import com.carlos.mongodb.entity.LogDocument;

import java.util.List;

/**
 * 日志文档 Service 接口
 * <p>扩展自 {@link BaseService}，继承完整的 mongo-plus CRUD 和 Lambda 查询能力</p>
 *
 * @author Carlos
 * @date 2026/3/15
 */
public interface LogDocumentService extends BaseService<LogDocument> {

    /**
     * 根据日志级别查询
     */
    List<LogDocument> findByLevel(String level);

    /**
     * 根据模块名称模糊查询
     */
    List<LogDocument> findByModuleLike(String module);

    /**
     * 根据用户ID查询
     */
    List<LogDocument> findByUserId(Long userId);

    /**
     * 根据是否成功查询
     */
    List<LogDocument> findBySuccess(Boolean success);
}
