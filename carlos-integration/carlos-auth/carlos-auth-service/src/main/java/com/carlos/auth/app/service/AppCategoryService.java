package com.carlos.auth.app.service;

import com.carlos.auth.app.pojo.dto.AppCategoryDTO;

import java.io.Serializable;
import java.util.List;

/**
 * 应用分类 业务逻辑服务
 *
 * @author Carlos
 * @date 2026-07-02
 */
public interface AppCategoryService {

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
     * 获取所有分类列表
     *
     * @return 分类列表
     */
    List<AppCategoryDTO> listAll();
}
