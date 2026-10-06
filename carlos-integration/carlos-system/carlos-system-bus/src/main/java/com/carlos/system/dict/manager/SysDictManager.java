package com.carlos.system.dict.manager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.carlos.system.dict.pojo.dto.SysDictDTO;
import com.carlos.system.dict.pojo.entity.SysDict;
import com.carlos.system.dict.pojo.param.SysDictPageParam;
import com.carlos.system.dict.pojo.vo.SysDictVO;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 系统字典 查询封装接口
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
public interface SysDictManager extends IService<SysDict> {


    /**
     * 添加字典
     *
     * @param dto 字典数据
     * @return boolean
     * @author carlos
     * @date 2021-11-22 14:49:00
     */
    boolean add(SysDictDTO dto);

    /**
     * 获取字典数量
     *
     * @param code 字典code
     * @param name 字典名称
     * @return java.lang.Long
     */
    Long count(String code, String name, Serializable excludeId);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.dict.pojo.vo.DictVO
     * @author carlos
     * @date 2021-11-22 14:49:00
     */
    SysDictDTO getDictById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @author carlos
     * @date 2021-11-22 14:49:00
     */
    IPage<SysDictVO> getPage(SysDictPageParam param);

    /**
     * 获取所有的字典（支持名称搜索）
     *
     * @param name 字典名称
     * @return com.carlos.dict.pojo.vo.DictListVO
     * @author carlos
     * @date 2021/11/25 15:06
     */
    List<SysDictDTO> listDict(String name);

    /**
     * 根据code获取id
     *
     * @param code 字典code
     * @return java.lang.Long
     * @author carlos
     * @date 2022/1/6 18:28
     */
    Serializable getIdByCode(String code);

    /**
     * 根据id获取字典
     *
     * @param ids 字典id
     * @return java.util.List<com.carlos.dict.pojo.dto.DictDTO>
     * @author carlos
     * @date 2022/1/6 18:28
     */
    List<SysDictDTO> listByDictIds(Set<Long> ids);
}
