package com.carlos.devkit.service;

import com.carlos.core.pagination.Paging;
import com.carlos.devkit.engine.model.TemplateGroupMeta;
import com.carlos.devkit.pojo.dto.TemplateGroupDTO;
import com.carlos.devkit.pojo.param.TemplateGroupCopyParam;
import com.carlos.devkit.pojo.param.TemplateGroupPageParam;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 模板组 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface TemplateGroupService {

    /**
     * 新增模板组
     *
     * @param dto 模板组数据
     * @return 模板组ID
     */
    Long addGroup(TemplateGroupDTO dto);

    /**
     * 修改模板组
     *
     * @param dto 模板组数据
     * @return 是否成功
     */
    boolean updateGroup(TemplateGroupDTO dto);

    /**
     * 删除模板组（连同其模板文件一并逻辑删除）
     *
     * @param id 模板组ID
     * @return 是否成功
     */
    boolean deleteGroup(Serializable id);

    /**
     * 模板组详情
     *
     * @param id 模板组ID
     * @return 模板组 DTO
     */
    TemplateGroupDTO getDetail(Serializable id);

    /**
     * 分页查询（回填模板文件数）
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<TemplateGroupDTO> getPage(TemplateGroupPageParam param);

    /**
     * 按类型查询全部模板组（回填模板文件数）
     *
     * @param templateType 模板类型，可空
     * @return DTO 列表
     */
    List<TemplateGroupDTO> listByType(Integer templateType);

    /**
     * 复制模板组（含模板文件）
     *
     * @param param 复制参数
     * @return 新模板组ID
     */
    Long copyGroup(TemplateGroupCopyParam param);

    /**
     * 切换启用状态
     *
     * @param id     模板组ID
     * @param status 状态
     * @return 是否成功
     */
    boolean toggleStatus(Serializable id, Integer status);

    /**
     * 取模板组元数据（生成用，校验存在且启用）
     *
     * @param groupId 模板组ID
     * @return 模板组元数据
     */
    TemplateGroupMeta getMeta(Serializable groupId);
}
