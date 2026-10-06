package com.carlos.devkit.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.devkit.pojo.dto.DataSourceDTO;
import com.carlos.devkit.pojo.entity.DevDataSource;
import com.carlos.devkit.pojo.param.DataSourcePageParam;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 受管数据源 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface DataSourceManager extends BaseService<DevDataSource> {

    /**
     * 新增数据源
     *
     * @param dto 数据源数据
     * @return 是否成功
     */
    boolean add(DataSourceDTO dto);

    /**
     * 修改数据源
     *
     * @param dto 数据源数据
     * @return 是否成功
     */
    boolean modify(DataSourceDTO dto);

    /**
     * 逻辑删除数据源
     *
     * @param ids 数据源ID集合
     * @return 是否成功
     */
    boolean delete(java.util.Collection<Serializable> ids);

    /**
     * 数据源详情（含密码密文）
     *
     * @param id 数据源ID
     * @return DTO，不存在返回 null
     */
    DataSourceDTO getDtoById(Serializable id);

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
     * 批量取「已配置密码」的数据源 ID
     *
     * <p>列表/下拉场景故意不查 {@code password_cipher}（避免密文进 DTO），
     * 因此「是否配了密码」需要这条只取主键的轻查询来判定。</p>
     *
     * @param ids 数据源ID集合
     * @return 已配置密码的 ID 集合
     */
    Set<Long> listIdsWithPassword(Collection<Long> ids);

    /**
     * 统计同名称的数据源数量（唯一性校验）
     *
     * @param dsName    数据源名称
     * @param excludeId 排除的ID
     * @return 数量
     */
    long countByName(String dsName, Serializable excludeId);
}
