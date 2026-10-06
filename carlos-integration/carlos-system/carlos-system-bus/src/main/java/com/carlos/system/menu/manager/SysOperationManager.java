package com.carlos.system.menu.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.system.enums.OperationType;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.menu.pojo.entity.SysOperation;
import com.carlos.system.menu.pojo.param.SysOperationPageParam;
import com.carlos.system.menu.pojo.vo.SysOperationVO;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 统一操作表 查询封装接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
public interface SysOperationManager extends BaseService<SysOperation> {

    /**
     * 新增操作
     *
     * @param dto 操作数据
     * @return boolean
     */
    boolean add(SysOperationDTO dto);

    /**
     * 删除操作
     *
     * @param id 操作id
     * @return boolean
     */
    boolean delete(Serializable id);

    /**
     * 修改操作信息
     *
     * @param dto 对象信息
     * @return boolean
     */
    boolean modify(SysOperationDTO dto);

    /**
     * 移动操作到指定菜单
     *
     * @param id     操作id
     * @param menuId 目标菜单ID，null 表示摘除菜单，成为独立接口
     * @return boolean
     */
    boolean move(Serializable id, Long menuId);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return SysOperationDTO
     */
    SysOperationDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @return Paging<SysOperationVO>
     */
    Paging<SysOperationVO> getPage(SysOperationPageParam param);

    /**
     * 按菜单ID查询操作列表
     *
     * @param menuId 菜单ID
     * @return 操作列表
     */
    List<SysOperationDTO> listByMenuId(Long menuId);

    /**
     * 按菜单ID集合查询操作列表
     *
     * @param menuIds 菜单ID集合
     * @return 操作列表
     */
    List<SysOperationDTO> listByMenuIds(Set<Long> menuIds);

    /**
     * 根据 perm_code 查询
     *
     * @param permCode 权限码
     * @return SysOperationDTO
     */
    SysOperationDTO getByPermCode(String permCode);

    /**
     * 根据 perm_codes 查询
     *
     * @param permCodes 权限码集合
     * @return 操作列表
     */
    List<SysOperationDTO> listByPermCodes(Set<String> permCodes);

    /**
     * 查询自动扫描的API列表
     *
     * @return API列表
     */
    List<SysOperationDTO> listAutoScanApis();


    /**
     * 查询所有操作列表
     *
     * @return 操作列表
     */
    List<SysOperationDTO> listAll();

    /**
     * 根据菜单ID和操作类型删除
     *
     * @param menuId  菜单ID
     * @param opType  操作类型
     * @return 是否成功
     */
    boolean deleteByMenuIdAndOpType(Long menuId, OperationType opType);

    /**
     * 查询指定菜单下的按钮列表（op_type=1）
     *
     * @param menuId 菜单ID
     * @return 按钮列表
     */
    List<SysOperationDTO> listButtonsByMenuId(Long menuId);
}
