package com.carlos.devkit.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.devkit.pojo.dto.TemplateGroupDTO;
import com.carlos.devkit.pojo.entity.DevTemplateGroup;
import com.carlos.devkit.pojo.param.TemplateGroupPageParam;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 模板组 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface TemplateGroupManager extends BaseService<DevTemplateGroup> {

    /**
     * 新增模板组
     *
     * @param dto 模板组数据
     * @return 是否成功
     */
    boolean add(TemplateGroupDTO dto);

    /**
     * 修改模板组
     *
     * @param dto 模板组数据
     * @return 是否成功
     */
    boolean modify(TemplateGroupDTO dto);

    /**
     * 逻辑删除模板组
     *
     * @param id 模板组ID
     * @return 是否成功
     */
    boolean delete(Serializable id);

    /**
     * 模板组详情
     *
     * @param id 模板组ID
     * @return DTO，不存在返回 null
     */
    TemplateGroupDTO getDtoById(Serializable id);

    /**
     * 分页查询
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<TemplateGroupDTO> getPage(TemplateGroupPageParam param);

    /**
     * 按类型查询全部模板组（下拉用）
     *
     * @param templateType 模板类型，可空表示不限
     * @return DTO 列表
     */
    List<TemplateGroupDTO> listByType(Integer templateType);

    /**
     * 统计同编码的模板组数量（唯一性校验）
     *
     * @param groupCode 模板组编码
     * @param excludeId 排除的ID（编辑场景）
     * @return 数量
     */
    long countByCode(String groupCode, Serializable excludeId);
}
