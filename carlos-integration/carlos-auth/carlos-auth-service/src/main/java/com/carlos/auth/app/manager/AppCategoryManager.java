package com.carlos.auth.app.manager;

import com.carlos.auth.app.pojo.dto.AppCategoryDTO;
import com.carlos.auth.app.pojo.entity.AppCategory;
import com.carlos.datasource.base.BaseService;

import java.io.Serializable;
import java.util.List;

/**
 * 应用分类 查询封装接口
 *
 * @author Carlos
 * @date 2026-07-02
 */
public interface AppCategoryManager extends BaseService<AppCategory> {

    /**
     * 新增应用分类
     *
     * @param dto 应用分类数据
     * @return 是否成功
     */
    boolean add(AppCategoryDTO dto);

    /**
     * 删除应用分类
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean delete(Serializable id);

    /**
     * 修改应用分类
     *
     * @param dto 应用分类数据
     * @return 是否成功
     */
    boolean modify(AppCategoryDTO dto);

    /**
     * 获取所有分类列表（已缓存）
     *
     * @return 分类列表
     */
    List<AppCategoryDTO> listAll();
}
