package com.carlos.devkit.service;

import com.carlos.core.pagination.Paging;
import com.carlos.devkit.pojo.dto.TemplateVersionDTO;
import com.carlos.devkit.pojo.param.TemplateVersionPageParam;

import java.io.Serializable;

/**
 * <p>
 * 模板版本历史 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface TemplateVersionService {

    /**
     * 版本分页
     *
     * @param param 分页参数
     * @return 分页结果（不含正文）
     */
    Paging<TemplateVersionDTO> getPage(TemplateVersionPageParam param);

    /**
     * 版本详情（含正文，供 diff）
     *
     * @param id 版本ID
     * @return 版本 DTO
     */
    TemplateVersionDTO getDetail(Serializable id);

    /**
     * 回滚到指定版本（回滚前先把当前内容落一版，避免回滚不可逆）
     *
     * @param versionId 版本ID
     * @return 是否成功
     */
    boolean rollback(Serializable versionId);
}
