package com.carlos.mongodb.service.impl;

import com.carlos.mongodb.base.BaseServiceImpl;
import com.carlos.mongodb.entity.LogDocument;
import com.carlos.mongodb.service.LogDocumentService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 日志文档 Service 实现类
 *
 * <p>继承 {@link BaseServiceImpl}，自动获得 Lambda 查询和分页能力。</p>
 *
 * @author Carlos
 * @date 2026/3/15
 */
@Service
public class LogDocumentServiceImpl extends BaseServiceImpl<LogDocument>
    implements LogDocumentService {

    public LogDocumentServiceImpl(MongoTemplate mongoTemplate) {
        super(mongoTemplate);
    }

    @Override
    public List<LogDocument> findByLevel(String level) {
        return lambdaQuery()
            .eq(LogDocument::getLevel, level)
            .list();
    }

    @Override
    public List<LogDocument> findByModuleLike(String module) {
        return lambdaQuery()
            .like(LogDocument::getModule, module)
            .list();
    }

    @Override
    public List<LogDocument> findByUserId(Long userId) {
        return lambdaQuery()
            .eq(LogDocument::getUserId, userId)
            .list();
    }

    @Override
    public List<LogDocument> findBySuccess(Boolean success) {
        return lambdaQuery()
            .eq(LogDocument::getSuccess, success)
            .list();
    }
}
