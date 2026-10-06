package com.carlos.system.region.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.system.region.pojo.dto.SysRegionDTO;
import com.carlos.system.region.pojo.entity.SysRegion;
import com.carlos.system.region.pojo.param.SysRegionPageParam;
import com.carlos.system.region.pojo.vo.SysRegionVO;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 行政区域划分 查询封装接口
 * </p>
 *
 * @author carlos
 * @date 2022-11-8 19:30:24
 */
public interface SysRegionManager extends BaseService<SysRegion> {

    /**
     * 新增行政区域划分
     *
     * @param dto 行政区域划分数据
     * @return boolean
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    boolean add(SysRegionDTO dto);

    /**
     * 删除行政区域划分
     *
     * @param id 行政区域划分id
     * @return boolean
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    boolean delete(Serializable id);

    /**
     * 修改行政区域划分信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    boolean modify(SysRegionDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.system.region.pojo.dto.SysRegionDTO
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    SysRegionDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @author carlos
     * @date 2022-11-8 19:30:24
     */
    Paging<SysRegionVO> getPage(SysRegionPageParam param);

    /**
     * 根据regionCode获取区域信息
     *
     * @author carlos
     * @date 2022-11-9 21:24:24
     */
    SysRegionDTO getByRegionCode(String regionCode);

    /**
     * 列出指定父级下的区域
     *
     * @param parentCode 父级code
     * @return java.util.List<com.carlos.system.region.pojo.dto.SysRegionDTO>
     * @author Carlos
     * @date 2022/11/16 17:33
     */
    List<SysRegionDTO> listByParentCode(String parentCode);

    /**
     * 根据父级编码列表获取区域列表
     *
     * @param codes 父级编码列表
     * @return java.util.List<com.carlos.system.region.pojo.dto.SysRegionDTO>
     * @author Carlos
     * @date 2025-12-09 09:39
     */
    List<SysRegionDTO> listByParentCodes(List<String> codes);

    /**
     * 计算指定父级下区域的数目
     *
     * @param parentCode 父级code
     * @return java.lang.Integer
     * @author Carlos
     * @date 2022/11/16 17:54
     */
    Long countByParentCode(String parentCode);

    /**
     * 获取区域列表
     *
     * @param parentId 参数0
     * @param name     参数1
     * @param code     参数2
     * @param detail   参数3
     * @return java.util.List<com.carlos.system.region.pojo.dto.SysRegionDTO>
     * @author Carlos
     * @date 2022/11/17 10:22
     */
    List<SysRegionDTO> getList(String parentId, String name, String code, boolean detail);

    /**
     * @Title: listAll
     * @Description: 获取所有
     * @Date: 2022/12/31 15:39
     * @Parameters: []
     * @Return java.util.List<com.carlos.common.dto.sys.SysRegionDTO>
     */
    List<SysRegionDTO> listAll();

    /**
     * 初始化缓存
     *
     * @author Carlos
     * @date 2023/2/13 13:43
     */
    void initCache();

    /**
     * @Title: addBatch
     * @Description: 批量保存
     * @Date: 2023/2/21 18:50
     * @Parameters: [regions]
     * @Return boolean
     */
    boolean addBatch(List<SysRegionDTO> regions);

    /**
     * 获取所有父级编码
     *
     * @param code 区域编码
     * @return java.util.List<java.lang.String>
     * @throws
     * @author Carlos
     * @date 2025-12-05 11:33
     */
    List<String> getParentsCode(String code);

    /**
     * 获取祖先链
     *
     * @param code 参数0
     * @return java.util.List<java.lang.String>
     */
    List<String> getAncestorIdsFromCache(String code, long limit);

    /**
     *  获取所有子级编码
     *
     * @param code 参数0
     * @return java.util.Set<java.lang.String>
     * @author Carlos
     * @date 2025-12-09 09:39
     */
    Set<String> getDescIdsFromCache(String code);

    /**
     * 清空缓存
     */
    long clearCache();


    /**
     * 从缓存中获取区域信息
     *
     * @param parentCodes 参数0
     * @param fields 参数1
     * @return java.util.List<com.carlos.system.region.pojo.dto.SysRegionDTO>
     * @author Carlos
     * @date 2025-12-10 09:22
     */
    List<SysRegionDTO> listRegionFromCache(List<String> parentCodes, List<String> fields);
}
