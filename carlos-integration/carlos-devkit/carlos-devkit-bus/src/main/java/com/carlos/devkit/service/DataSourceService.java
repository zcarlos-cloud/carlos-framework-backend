package com.carlos.devkit.service;

import com.carlos.core.pagination.Paging;
import com.carlos.devkit.engine.model.ConnectionTestResult;
import com.carlos.devkit.engine.model.TargetDatabase;
import com.carlos.devkit.pojo.dto.DataSourceDTO;
import com.carlos.devkit.pojo.param.DataSourcePageParam;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 受管数据源 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface DataSourceService {

    /**
     * 新增数据源（口径明文经 SM4 加密后落库）
     *
     * @param dto 数据源数据
     * @return 数据源ID
     */
    Long addDataSource(DataSourceDTO dto);

    /**
     * 修改数据源（密码为空表示保持原值）
     *
     * @param dto 数据源数据
     * @return 是否成功
     */
    boolean updateDataSource(DataSourceDTO dto);

    /**
     * 批量删除数据源
     *
     * @param ids 数据源ID集合
     * @return 是否成功
     */
    boolean deleteDataSources(Collection<Long> ids);

    /**
     * 数据源详情（含密码密文，仅后端使用）
     *
     * @param id 数据源ID
     * @return 数据源 DTO
     */
    DataSourceDTO getDetail(Serializable id);

    /**
     * 分页查询
     *
     * @param param 分页参数
     * @return 分页结果
     */
    Paging<DataSourceDTO> getPage(DataSourcePageParam param);

    /**
     * 全部启用的数据源（下拉用）
     *
     * @return DTO 列表
     */
    List<DataSourceDTO> listEnabled();

    /**
     * 测试连接
     *
     * @param dto       表单连接信息；{@code id} 非空且密码为空时使用库中已存密码
     * @return 测试结果
     */
    ConnectionTestResult testConnection(DataSourceDTO dto);

    /**
     * 解析目标数据库连接信息（解密密码，校验启用状态）
     *
     * @param dsId   数据源ID
     * @param schema 覆盖 schema / 库（可空，取数据源默认值）
     * @return 目标数据库
     */
    TargetDatabase resolveTarget(Serializable dsId, String schema);
}
