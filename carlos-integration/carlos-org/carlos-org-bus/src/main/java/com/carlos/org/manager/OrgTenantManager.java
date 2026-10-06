package com.carlos.org.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.org.pojo.dto.OrgTenantDTO;
import com.carlos.org.pojo.entity.OrgTenant;
import com.carlos.org.pojo.param.OrgTenantPageParam;

import java.io.Serializable;

/**
 * <p>
 * 租户 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
public interface OrgTenantManager extends BaseService<OrgTenant> {

    /**
     * 新增租户
     *
     * @param dto 租户数据
     * @return boolean
     */
    boolean add(OrgTenantDTO dto);

    /**
     * 删除租户
     *
     * @param id 租户id
     * @return boolean
     */
    boolean delete(Serializable id);

    /**
     * 修改租户信息
     *
     * @param dto 对象信息
     * @return boolean
     */
    boolean modify(OrgTenantDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return OrgTenantDTO
     */
    OrgTenantDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @return Paging<OrgTenantDTO>
     */
    Paging<OrgTenantDTO> getPage(OrgTenantPageParam param);

    /**
     * 根据租户编码获取租户
     *
     * @param tenantCode 租户编码
     * @return OrgTenantDTO
     */
    OrgTenantDTO getByTenantCode(String tenantCode);

}
