package com.carlos.devkit.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.devkit.pojo.dto.TemplateVersionDTO;
import com.carlos.devkit.pojo.entity.DevTemplateVersion;
import com.carlos.devkit.pojo.param.TemplateVersionPageParam;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 模板版本历史 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface TemplateVersionManager extends BaseService<DevTemplateVersion> {

    /**
     * 追加一个版本
     *
     * @param dto 版本数据
     * @return 是否成功
     */
    boolean add(TemplateVersionDTO dto);

    /**
     * 版本分页
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<TemplateVersionDTO> getPage(TemplateVersionPageParam param);

    /**
     * 版本详情（含正文）
     *
     * @param id 版本ID
     * @return DTO，不存在返回 null
     */
    TemplateVersionDTO getDtoById(Serializable id);

    /**
     * 取模板下一个版本号
     *
     * @param templateId 模板ID
     * @return 版本号（无历史时返回 1）
     */
    int nextVersionNo(Serializable templateId);

    /**
     * 查询超出保留数量的旧版本（用于清理）
     *
     * @param templateId 模板ID
     * @param keepCount  保留版本数
     * @return 待清理版本ID
     */
    List<Long> listExpiredVersionIds(Serializable templateId, int keepCount);

    /**
     * 逻辑删除指定版本
     *
     * @param ids 版本ID集合
     * @return 是否成功
     */
    boolean delete(java.util.Collection<Serializable> ids);
}
