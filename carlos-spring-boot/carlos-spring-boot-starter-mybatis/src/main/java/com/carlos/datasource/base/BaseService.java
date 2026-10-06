package com.carlos.datasource.base;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.IService;
import com.carlos.core.param.ParamPage;
import com.carlos.datasource.pagination.PageInfo;

/**
 * <p>
 * 基于 Mybatis-Plus 的公共 Service 接口（增强版）
 * </p>
 *
 * <p>
 * 继承自 IService 和 BatchService，提供完整的 CRUD 和批量操作能力
 * </p>
 *
 * @author carlos
 * @date 2020/4/11 22:59
 */
public interface BaseService<T> extends IService<T>, BatchService<T> {

    /**
     * 获取查询构造器
     *
     * @return LambdaQueryWrapper
     */
    default LambdaQueryWrapper<T> queryWrapper() {
        return Wrappers.lambdaQuery(getEntityClass());
    }

    /**
     * 获取更新构造器
     *
     * @return LambdaUpdateWrapper
     */
    default LambdaUpdateWrapper<T> updateWrapper() {
        return Wrappers.lambdaUpdate(getEntityClass());
    }

    /**
     * 分页信息获取
     *
     * @param param 分页参数
     * @return PageInfo
     */
    default PageInfo<T> pageInfo(ParamPage param) {
        return new PageInfo<>(param);
    }

    /**
     * 分页信息获取（带排序映射）
     *
     * @param param 分页参数
     * @return PageInfo
     */
    default PageInfo<T> pageInfo(ParamPage param,
                                 com.carlos.datasource.pagination.OrderMapping orderMapping) {
        return new PageInfo<>(param, orderMapping);
    }
}
