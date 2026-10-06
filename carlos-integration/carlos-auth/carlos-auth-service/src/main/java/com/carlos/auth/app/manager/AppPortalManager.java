package com.carlos.auth.app.manager;

import com.carlos.auth.app.pojo.dto.AppPortalAppDTO;

import java.util.List;

/**
 * 应用门户查询 Manager 接口
 *
 * @author Carlos
 * @date 2026-07-02
 */
public interface AppPortalManager {

    /**
     * 获取所有门户可见的应用列表（已缓存）
     *
     * @return 门户可见应用列表
     */
    List<AppPortalAppDTO> listPortalVisibleApps();
}
