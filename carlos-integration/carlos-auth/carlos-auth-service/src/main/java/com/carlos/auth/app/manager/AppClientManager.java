package com.carlos.auth.app.manager;

import com.carlos.auth.app.pojo.dto.AppClientDTO;
import com.carlos.auth.app.pojo.entity.AppClient;
import com.carlos.auth.app.pojo.param.AppClientPageParam;
import com.carlos.auth.app.pojo.vo.AppClientPageVO;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 应用信息 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
public interface AppClientManager extends BaseService<AppClient> {

    /**
     * 新增应用信息
     *
     * @param dto 应用信息数据
     * @return boolean
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    boolean add(AppClientDTO dto);

    /**
     * 删除应用信息
     *
     * @param id 应用信息id
     * @return boolean
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    boolean delete(Serializable id);

    /**
     * 修改应用信息信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    boolean modify(AppClientDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.oauth.client.pojo.dto.AppClientDTO
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    AppClientDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param param 分页参数
     * @author Carlos
     * @date 2025-3-12 14:00:14
     */
    Paging<AppClientPageVO> getPage(AppClientPageParam param);

    /**
     * 获取应用列表
     *
     * @param keyword 查询关键字
     * @return java.util.List<com.carlos.oauth.client.pojo.dto.AppClientDTO>
     * @author Carlos
     * @date 2025-03-26 10:28
     */
    List<AppClientDTO> listByKeyword(String keyword);

    /**
     * 根据appKey获取应用信息
     *
     * @param appKey 应用id
     * @return com.carlos.oauth.client.pojo.dto.AppClientDTO
     * @author Carlos
     * @date 2025-03-12 14:38
     */
    AppClientDTO getByAppkey(String appKey);

    /**
     * 初始化缓存
     *
     * @author Carlos
     * @date 2023/2/13 13:43
     */
    void initCache();

    /**
     * 获取所有应用信息
     *
     * @return java.util.List<com.carlos.oauth.client.pojo.dto.AppClientDTO>
     * @author Carlos
     * @date 2025-03-27 13:44
     */
    List<AppClientDTO> listAll();

    /**
     * getByClientName
     *
     * @param appName 参数0
     * @return com.carlos.oauth.client.pojo.dto.AppClientDTO
     * @throws
     * @author Carlos
     * @date 2025-03-28 17:08
     */
    AppClientDTO getByClientName(String appName);
}
