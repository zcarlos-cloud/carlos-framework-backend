package com.carlos.boot.web.resource;

import com.carlos.boot.web.resource.model.ResourceApp;

/**
 * 资源存储接口，供业务模块实现自定义持久化策略。
 *
 * @author Carlos
 * @since 3.0.0
 */
public interface ResourceStore {

    /**
     * 保存扫描到的应用资源。
     *
     * @param app 应用资源
     */
    void save(ResourceApp app);
}
