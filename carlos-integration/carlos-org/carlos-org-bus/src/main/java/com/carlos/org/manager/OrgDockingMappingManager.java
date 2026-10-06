package com.carlos.org.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.org.pojo.dto.OrgDockingMappingDTO;
import com.carlos.org.pojo.entity.OrgDockingMapping;
import com.carlos.org.pojo.enums.OrgDockingTypeEnum;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 用户信息对接关联表 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2025-2-27 15:41:32
 */
public interface OrgDockingMappingManager extends BaseService<OrgDockingMapping> {

    /**
     * 新增用户信息对接关联表
     *
     * @param dto 用户信息对接关联表数据
     * @return boolean
     * @author Carlos
     * @date 2025-2-27 15:41:32
     */
    boolean add(OrgDockingMappingDTO dto);

    /**
     * 删除用户信息对接关联表
     *
     * @param id 用户信息对接关联表id
     * @return boolean
     * @author Carlos
     * @date 2025-2-27 15:41:32
     */
    boolean delete(Serializable id);

    /**
     * 获取映射信息
     *
     * @param type 参数0
     * @param targetCode 参数1
     * @param targetId 参数2
     * @return com.carlos.org.pojo.dto.OrgDockingMappingDTO
     * @author Carlos
     * @date 2025-02-28 11:23
     */
    OrgDockingMappingDTO getDockingMapping(OrgDockingTypeEnum type, String targetCode, String targetId);

    boolean deleteByTargetId(String targetId);

    OrgDockingMappingDTO getSystemIdByTargetId(String targetId);

    List<OrgDockingMappingDTO> getDockingMappingList(OrgDockingTypeEnum type);

    List<OrgDockingMappingDTO> listByTargetIds(Set<String> targetIds);

    OrgDockingMappingDTO getDockingMappingByTargetId(String targetId);


    List<OrgDockingMappingDTO> listBySystemIds(Set<String> systemIds);
}
