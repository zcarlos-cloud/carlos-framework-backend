package com.carlos.system.dict.manager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.carlos.system.dict.pojo.dto.SysDictItemDTO;
import com.carlos.system.dict.pojo.entity.SysDictItem;
import com.carlos.system.dict.pojo.param.SysDictItemPageParam;
import com.carlos.system.dict.pojo.vo.SysDictItemVO;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 系统字典详情 查询封装接口
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
public interface SysDictItemManager extends IService<SysDictItem> {

    /**
     * 添加数据
     *
     * @param dto 数据
     * @return boolean
     * @author carlos
     * @date 2021-11-22 14:49:00
     */
    boolean add(SysDictItemDTO dto);

    /**
     * 获取数据数量
     *
     * @param dictId   字典id
     * @param excludeId 排除的id
     * @param name     名称
     * @param code     编码
     * @return long
     * @author carlos
     * @date 2021-11-22 14:49:00
     */
    long count(Serializable dictId, Serializable excludeId, String name, String code);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.dict.pojo.vo.DictItemVO
     * @author carlos
     * @date 2021-11-22 14:49:00
     */
    SysDictItemDTO getItemById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @author carlos
     * @date 2021-11-22 14:49:00
     */
    IPage<SysDictItemVO> getPage(SysDictItemPageParam param);

    /**
     * 获取字典选项的字典id
     *
     * @param id 字典选项id
     * @return java.lang.Long
     */
    Serializable getDictIdById(Serializable id);

    /**
     * 获取下拉列表
     *
     * @param dictIds 字典id
     * @param name   搜索名称
     * @param onlyEnable 是否只查询启用的
     * @return java.util.List<com.carlos.dict.pojo.vo.DictItemListVO>
     * @author carlos
     * @date 2021/11/25 16:54
     */
    List<SysDictItemDTO> listItems(Set<Serializable> dictIds, String name, boolean onlyEnable);

    /**
     * 根据字典编码获取字典选项
     *
     * @param dictCode 字典编码
     * @return java.util.List<com.carlos.dict.pojo.vo.DictItemVO>
     * @author carlos
     * @date 2022/1/4 11:13
     */
    List<SysDictItemDTO> listByDictCode(String dictCode);

    /**
     * 根据字典选项id获取字典选项
     *
     * @param ids 字典选项id
     * @return java.util.List<com.carlos.dict.pojo.vo.DictItemVO>
     * @author carlos
     * @date 2022/1/4 11:13
     */
    List<SysDictItemDTO> listByItemIds(Set<Long> ids);
}
