package com.carlos.system.menu.service;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.system.enums.OperationType;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.menu.manager.SysMenuManager;
import com.carlos.system.menu.manager.SysOperationManager;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.menu.pojo.param.SysOperationPageParam;
import com.carlos.system.menu.pojo.vo.SysOperationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 统一操作表 业务接口实现类
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationService {

    private final SysOperationManager sysOperationManager;
    private final SysMenuManager sysMenuManager;

    /**
     * 新增操作
     *
     * @param dto 操作数据
     */
    public void addOperation(SysOperationDTO dto) {
        boolean success = sysOperationManager.add(dto);
        if (!success) {
            return;
        }
    }

    /**
     * 删除操作
     *
     * @param ids 操作id集合
     */
    public void deleteOperation(Set<String> ids) {
        for (Serializable id : ids) {
            boolean success = sysOperationManager.delete(id);
            if (!success) {
                continue;
            }
        }
    }

    /**
     * 更新操作
     *
     * @param dto 操作数据
     */
    public void updateOperation(SysOperationDTO dto) {
        boolean success = sysOperationManager.modify(dto);
        if (!success) {
            return;
        }
    }

    /**
     * 根据ID查询
     *
     * @param id 操作id
     * @return 操作DTO
     */
    public SysOperationDTO getById(Serializable id) {
        return sysOperationManager.getDtoById(id);
    }

    /**
     * 分页查询
     *
     * @param param 分页参数
     * @return 分页结果
     */
    public Paging<SysOperationVO> getPage(SysOperationPageParam param) {
        return sysOperationManager.getPage(param);
    }

    /**
     * 根据菜单ID查询操作列表
     *
     * @param menuId 菜单ID
     * @return 操作列表
     */
    public List<SysOperationDTO> listByMenuId(Long menuId) {
        return sysOperationManager.listByMenuId(menuId);
    }

    /**
     * 移动操作到指定菜单
     *
     * <p>menuId 为 null 表示摘除菜单，成为独立接口；按钮（{@link OperationType#BUTTON}）
     * 必须挂在菜单下，不允许 menuId 为 null。menuId 非 null 时目标菜单必须存在。</p>
     *
     * @param id     操作id
     * @param menuId 目标菜单ID，null 表示独立接口
     * @return 是否成功
     */
    public Boolean moveOperation(Long id, Long menuId) {
        SysOperationDTO operation = sysOperationManager.getDtoById(id);
        if (operation == null) {
            throw CommonErrorCode.NOT_FOUND.exception("操作不存在，id=" + id);
        }
        if (menuId == null && OperationType.BUTTON == operation.getOpType()) {
            throw CommonErrorCode.PARAM_VALIDATION_ERROR.exception("按钮必须挂在菜单下，不能摘除菜单");
        }
        if (menuId != null && sysMenuManager.getById(menuId) == null) {
            throw SystemErrorCode.SYS_MENU_NOT_FOUND.exception();
        }
        return sysOperationManager.move(id, menuId);
    }

    /**
     * 根据菜单ID集合查询操作列表
     *
     * @param menuIds 菜单ID集合
     * @return 操作列表
     */
    public List<SysOperationDTO> listByMenuIds(Set<Long> menuIds) {
        return sysOperationManager.listByMenuIds(menuIds);
    }

    /**
     * 根据权限码查询
     *
     * @param permCode 权限码
     * @return 操作DTO
     */
    public SysOperationDTO getByPermCode(String permCode) {
        return sysOperationManager.getByPermCode(permCode);
    }

    /**
     * 根据权限码集合查询
     *
     * @param permCodes 权限码集合
     * @return 操作列表
     */
    public List<SysOperationDTO> listByPermCodes(Set<String> permCodes) {
        return sysOperationManager.listByPermCodes(permCodes);
    }

    /**
     * 查询自动扫描的API列表
     *
     * @return API列表
     */
    public List<SysOperationDTO> listAutoScanApis() {
        return sysOperationManager.listAutoScanApis();
    }

}
