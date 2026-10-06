package com.carlos.system.menu.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.constant.CoreConstant;
import com.carlos.core.pagination.Paging;
import com.carlos.system.enums.*;
import com.carlos.system.menu.convert.SysMenuConvert;
import com.carlos.system.menu.manager.SysMenuManager;
import com.carlos.system.menu.manager.SysOperationManager;
import com.carlos.system.menu.pojo.dto.SysMenuButtonDTO;
import com.carlos.system.menu.pojo.dto.SysMenuDTO;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.menu.pojo.param.SysMenuPageParam;
import com.carlos.system.menu.pojo.vo.SysMenuButtonVO;
import com.carlos.system.menu.pojo.vo.SysMenuVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 系统菜单 业务接口实现类
 * </p>
 *
 * @author carlos
 * @date 2021-12-28 15:26:57
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuManager menuManager;
    private final SysOperationManager sysOperationManager;

    /** 菜单树最大层级，用于回溯父级链时的死循环兜底 */
    private static final int MAX_MENU_DEPTH = 32;

    /**
     * 新增系统菜单
     *
     * @param dto 系统菜单数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void addMenu(SysMenuDTO dto) {
        if (ObjectUtil.isEmpty(dto.getParentId())) {
            dto.setParentId(CoreConstant.PARENT_LONG_0);
        }
        Optional.ofNullable(dto.getMeta()).ifPresent(i -> {
            if (StrUtil.isBlank(i)) {
                dto.setMeta(null);
            }
        });

        dto.setMenuLevel(getMenuLevel(dto.getParentId()));
        boolean success = menuManager.add(dto);
        if (!success) {
            throw SystemErrorCode.SYS_SYSTEM_ERROR.exception("菜单保存失败");
        }

        // 同步按钮权限到 sys_operation
        syncButtons(dto.getId(), dto.getButtons());
    }

    /**
     * 删除系统菜单
     *
     * @param ids 系统菜单id
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Set<Long> ids) {
        for (Serializable id : ids) {
            checkSubMenus(id);
            SysMenuDTO menu = menuManager.getDtoById(id);
            if (menu != null && menu.getMenuStatus() == MenuStatus.ENABLED) {
                throw SystemErrorCode.SYS_MENU_SYSTEM_CANNOT_DELETE.exception();
            }
            // 级联删除关联的按钮权限（op_type=1）
            sysOperationManager.deleteByMenuIdAndOpType((Long) id, OperationType.BUTTON);
            boolean success = menuManager.delete(id);
            if (!success) {
                throw SystemErrorCode.SYS_SYSTEM_ERROR.exception("菜单删除失败");
            }
        }
    }

    /**
     * 修改系统菜单信息
     *
     * @param dto 对象信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(SysMenuDTO dto) {
        SysMenuDTO existing = menuManager.getDtoById(dto.getId());
        // 历史数据里顶层菜单的 parent_id 可能是 NULL 而非 0，比较前先归一
        Long existingParentId = existing == null || existing.getParentId() == null
            ? CoreConstant.PARENT_LONG_0
            : existing.getParentId();

        if (ObjectUtil.isEmpty(dto.getParentId())) {
            dto.setParentId(CoreConstant.PARENT_LONG_0);
        }
        Optional.ofNullable(dto.getMeta()).ifPresent(i -> {
            if (StrUtil.isBlank(i)) {
                dto.setMeta(null);
            }
        });

        // 层级只由父级决定，父级没变就不必重算；这样也避开了
        // 「库里已有脏环时，连改个名称都会被环校验拦住」的误报
        if (!Objects.equals(dto.getParentId(), existingParentId)) {
            checkParentCycle(dto.getId(), dto.getParentId());

            // 只重算被移动的节点本身，其子孙的 menu_level 不会级联更新。
            // 该字段目前无下游读取方（仅 getMenuLevel 用来给新建菜单定层级），
            // 影响面有限；若要彻底正确需追加重算整棵子树。
            dto.setMenuLevel(getMenuLevel(dto.getParentId()));
        }

        boolean success = menuManager.modify(dto);
        if (!success) {
            throw SystemErrorCode.SYS_SYSTEM_ERROR.exception("菜单更新失败");
        }

        // 同步按钮权限：先删除再重建
        sysOperationManager.deleteByMenuIdAndOpType(dto.getId(), OperationType.BUTTON);
        syncButtons(dto.getId(), dto.getButtons());
    }

    /**
     * 同步按钮权限到 sys_operation
     *
     * @param menuId  菜单ID
     * @param buttons 按钮列表
     */
    private void syncButtons(Long menuId, List<SysMenuButtonDTO> buttons) {
        if (menuId == null || buttons == null || buttons.isEmpty()) {
            return;
        }
        int sort = 1;
        for (SysMenuButtonDTO button : buttons) {
            if (StrUtil.isBlank(button.getCode())) {
                continue;
            }
            SysOperationDTO op = new SysOperationDTO();
            op.setOpType(OperationType.BUTTON);
            op.setMenuId(menuId);
            op.setPermCode(button.getCode());
            op.setOpName(button.getDesc());
            op.setOpStatus(OperationStatus.ENABLED);
            op.setOrderNum(sort++);
            sysOperationManager.add(op);
        }
    }

    /**
     * 获取菜单级数
     *
     * @param parentId 父id
     * @return 菜单级数
     */
    public Integer getMenuLevel(Serializable parentId) {
        // parentId 实际类型为 Long，历史实现写作 "0".equals(parentId)，对 Long 恒为 false，
        // 导致顶层菜单新增时误入查库分支并抛 NOT_FOUND。统一按字符串比较，
        // 同时兼容 Long 0L 与 String "0" 两种入参。
        if (parentId == null || CoreConstant.PARENT_LONG_0.toString().equals(parentId.toString())) {
            return 1;
        }
        SysMenuDTO dto = menuManager.getDtoById(parentId);
        if (dto == null) {
            log.error("Not found menu data, id:{}", parentId);
            throw SystemErrorCode.NOT_FOUND.exception("数据不存在");
        }
        // sys_menu.menu_level 可空（建表未加 NOT NULL，历史数据/导入可能为空），
        // 直接拆箱会抛裸 NPE；缺失时按第 1 层兜底，避免整个更新流程 500
        if (dto.getMenuLevel() == null) {
            log.warn("Parent menu level is null, fallback to 1. menuId:{}", parentId);
            return 1;
        }
        return dto.getMenuLevel() + 1;
    }

    /**
     * 校验新的上级菜单不会形成环
     *
     * 把菜单挂到自身或自己的子孙下面会让父子链成环：环上的节点谁都回溯不到根，
     * 既不是根节点也挂不到任何真实父节点上，整棵子树在树形查询里会直接消失。
     *
     * 做法是从目标父级沿 parentId 向上回溯，途中命中当前菜单即判定成环。
     * 回溯到根（parentId 为 0）即终止，深度天然有界；再用 {@link #MAX_MENU_DEPTH}
     * 兜底历史上可能已存在的脏数据环，避免死循环。
     *
     * @param menuId      当前菜单id
     * @param newParentId 目标父级id，调用前已归一化（空值会被替换为 0）
     */
    private void checkParentCycle(Long menuId, Long newParentId) {
        if (menuId == null || newParentId == null || CoreConstant.PARENT_LONG_0.equals(newParentId)) {
            return;
        }

        Long cursor = newParentId;

        for (int depth = 0; cursor != null && !CoreConstant.PARENT_LONG_0.equals(cursor); depth++) {
            if (depth >= MAX_MENU_DEPTH) {
                log.error("Menu parent chain too deep or cyclic, menuId:{}, newParentId:{}", menuId, newParentId);
                throw SystemErrorCode.SYS_SYSTEM_ERROR.exception("菜单层级异常，请检查现有父级配置");
            }
            if (cursor.equals(menuId)) {
                if (depth == 0) {
                    throw SystemErrorCode.SYS_MENU_PARENT_CANNOT_BE_SELF.exception();
                }
                throw SystemErrorCode.SYS_MENU_PARENT_CANNOT_BE_DESCENDANT.exception();
            }
            SysMenuDTO parent = menuManager.getDtoById(cursor);
            cursor = parent == null ? null : parent.getParentId();
        }
    }

    /**
     * 检查菜单下的直接子菜单是否为空
     *
     * @param id 菜单id
     */
    private void checkSubMenus(Serializable id) {
        if (menuManager.getSubMenuCount(id) > 0) {
            throw SystemErrorCode.SYS_MENU_HAS_CHILDREN.exception();
        }
    }

    /**
     * 根据ids获取菜单列表
     *
     * @param ids 菜单id集合
     * @return 菜单列表
     */
    public List<SysMenuDTO> getByIds(Set<String> ids) {
        return menuManager.getDtoByIds(ids);
    }

    /**
     * 获取所有的菜单
     *
     * @return 菜单列表
     */
    public List<SysMenuDTO> all() {
        return menuManager.listAll();
    }

    /**
     * 根据客户端类型获取菜单列表
     *
     * @param menuType 客户端类型
     * @return 菜单列表
     */
    public List<SysMenuDTO> listByType(MenuType menuType) {
        return menuManager.listByType(menuType);
    }

    /**
     * 获取所有页面路由名
     *
     * @return 路由名列表
     */
    public List<String> getAllPages() {
        return menuManager.listAllPageNames();
    }

    /**
     * 获取菜单列表（包含 buttons，从 sys_operation 组装）
     *
     * @param menuCategory 菜单类别
     * @return 菜单列表
     */
    public List<SysMenuDTO> listWithButtons(MenuCategory menuCategory) {
        List<SysMenuDTO> menus = menuManager.listByCategory(menuCategory);
        if (menus == null || menus.isEmpty()) {
            return menus;
        }
        for (SysMenuDTO menu : menus) {
            List<SysOperationDTO> ops = sysOperationManager.listButtonsByMenuId(menu.getId());
            if (ops != null && !ops.isEmpty()) {
                List<SysMenuButtonDTO> buttons = ops.stream()
                    .map(op -> new SysMenuButtonDTO(op.getPermCode(), op.getOpName()))
                    .collect(Collectors.toList());
                menu.setButtons(buttons);
            }
        }
        return menus;
    }

    /**
     * 分页查询菜单列表（包含 buttons）
     *
     * @param param 分页参数
     * @return 分页结果
     */
    public Paging<SysMenuVO> getMenuPageWithButtons(SysMenuPageParam param) {
        Paging<SysMenuVO> page = menuManager.getPage(param);
        if (page == null || page.getRecords() == null || page.getRecords().isEmpty()) {
            return page;
        }
        this.attachButtons(page.getRecords());
        return page;
    }

    /**
     * 全部菜单（含按钮权限）
     *
     * <p>
     * 管理后台的菜单树必须**一次拿到全量菜单**：树形层级与搜索都要求完整数据 ——
     * 后端 keyword 过滤只返回命中行、父级不在结果里，客户端要补出祖先链，无论如何都得先有全量。
     * </p>
     *
     * <p>
     * 此前前端走的是 {@code /sys/menu/pageWithButtons?size=50}，而库中菜单早已超过 50 条 ——
     * **第 51 条起静默消失**，既不在树上、也搜不到。故新增本端点取代「传个大 size」这种约定。
     * 命名对齐既有的 {@code /sys/module/all} 与 {@code /sys/config/all}。
     * </p>
     *
     * @return 全部菜单（按 orderNum 升序，同级再按 createTime 倒序，与树形展示一致）
     */
    public List<SysMenuVO> getAllMenusWithButtons() {
        List<SysMenuVO> menus = SysMenuConvert.INSTANCE.toListVO(menuManager.listAll());
        if (CollUtil.isEmpty(menus)) {
            return menus;
        }
        menus.sort(Comparator.comparing(SysMenuVO::getOrderNum, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(SysMenuVO::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        this.attachButtons(menus);
        return menus;
    }

    /**
     * 为菜单补齐按钮权限
     *
     * <p>
     * 按钮来自 {@code sys_operation}（按 menuId 关联），编辑抽屉要靠它回填既有的按钮行 ——
     * 缺了 buttons，一次编辑保存就会把该菜单下已有的按钮权限**静默清空**。
     * </p>
     *
     * @param menus 待补齐的菜单（原地写入 buttons）
     */
    private void attachButtons(List<SysMenuVO> menus) {
        for (SysMenuVO menu : menus) {
            List<SysOperationDTO> ops = sysOperationManager.listButtonsByMenuId(menu.getId());
            if (ops != null && !ops.isEmpty()) {
                List<SysMenuButtonVO> buttons = ops.stream()
                    .map(op -> new SysMenuButtonVO(op.getPermCode(), op.getOpName()))
                    .collect(Collectors.toList());
                menu.setButtons(buttons);
            }
        }
    }
}
