package com.carlos.system.menu.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.system.menu.pojo.dto.SysModuleDTO;
import com.carlos.system.menu.pojo.entity.SysModule;
import com.carlos.system.menu.pojo.param.SysModulePageParam;
import com.carlos.system.menu.pojo.vo.SysModuleVO;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 功能模块 查询封装接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
public interface SysModuleManager extends BaseService<SysModule> {

    /**
     * 根据模块编码获取模块
     *
     * @param moduleCode 模块编码
     * @return SysModule
     */
    SysModule getByModuleCode(String moduleCode);

    /**
     * 获取全部启用的模块
     *
     * @return 模块列表
     */
    List<SysModule> listEnabled();

    /**
     * 获取**全部**模块（含已禁用）
     *
     * <p>
     * 与 {@link #listEnabled()} 的区别：管理后台的模块树必须能看到禁用模块 ——
     * 否则禁用一个模块后它就从这个列表里消失，再也改不回来。
     * 同理，编辑某个子模块时，「上级模块」的可选项也不能只列启用的，
     * 否则其父模块一旦被禁用就会从下拉里消失、把既有父子关系弄丢。
     * </p>
     *
     * @return 模块列表（按 orderNum 升序）
     */
    List<SysModule> listAll();

    /**
     * 新增功能模块
     *
     * @param dto 数据
     * @return boolean
     */
    boolean add(SysModuleDTO dto);

    /**
     * 删除功能模块
     *
     * @param id 主键
     * @return boolean
     */
    boolean delete(Long id);

    /**
     * 修改功能模块
     *
     * @param dto 数据
     * @return boolean
     */
    boolean modify(SysModuleDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键
     * @return SysModuleDTO
     */
    SysModuleDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @return Paging<SysModuleVO>
     */
    Paging<SysModuleVO> getPage(SysModulePageParam param);

}
