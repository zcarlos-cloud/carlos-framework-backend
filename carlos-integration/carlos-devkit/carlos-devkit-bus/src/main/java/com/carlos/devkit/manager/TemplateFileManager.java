package com.carlos.devkit.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.devkit.pojo.dto.TemplateFileDTO;
import com.carlos.devkit.pojo.entity.DevTemplate;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 模板文件 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface TemplateFileManager extends BaseService<DevTemplate> {

    /**
     * 新增模板文件
     *
     * @param dto 模板文件数据
     * @return 是否成功
     */
    boolean add(TemplateFileDTO dto);

    /**
     * 修改模板文件
     *
     * @param dto 模板文件数据
     * @return 是否成功
     */
    boolean modify(TemplateFileDTO dto);

    /**
     * 逻辑删除模板文件
     *
     * @param ids 文件ID集合
     * @return 是否成功
     */
    boolean delete(Collection<Serializable> ids);

    /**
     * 模板文件详情（含正文）
     *
     * @param id 模板文件ID
     * @return DTO，不存在返回 null
     */
    TemplateFileDTO getDtoById(Serializable id);

    /**
     * 查询模板组下全部文件（含正文，生成时用）
     *
     * @param groupId 模板组ID
     * @return DTO 列表
     */
    List<TemplateFileDTO> listByGroupId(Serializable groupId);

    /**
     * 查询模板组下启用的文件（生成时用，不含正文）
     *
     * @param groupId 模板组ID
     * @return DTO 列表
     */
    List<TemplateFileDTO> listEnabledByGroupId(Serializable groupId);

    /**
     * 统计模板组下文件数（批量）
     *
     * @param groupIds 模板组ID集合
     * @return groupId → 文件数
     */
    java.util.Map<Long, Integer> countByGroupIds(Collection<Long> groupIds);

    /**
     * 统计同路径文件数量（唯一性校验）
     *
     * @param groupId   模板组ID
     * @param filePath  文件路径
     * @param excludeId 排除的ID
     * @return 数量
     */
    long countByPath(Serializable groupId, String filePath, Serializable excludeId);

    /**
     * 查询同路径文件
     *
     * @param groupId  模板组ID
     * @param filePath 文件路径
     * @return DTO，不存在返回 null
     */
    TemplateFileDTO getByPath(Serializable groupId, String filePath);

    /**
     * 逻辑删除模板组下全部文件
     *
     * @param groupId 模板组ID
     * @return 是否成功
     */
    boolean deleteByGroupId(Serializable groupId);
}
