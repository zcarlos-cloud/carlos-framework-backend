package com.carlos.boot.web.resource;

import com.carlos.boot.web.resource.model.ResourceApp;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认资源存储实现（空操作），业务模块可通过 {@code @Bean} 覆盖以自定义持久化。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
public class DefaultResourceStore implements ResourceStore {

    @Override
    public void save(ResourceApp app) {
        log.debug("DefaultResourceStore: resource scan result ignored ({} groups)",
            app.getGroups() != null ? app.getGroups().size() : 0);
    }
}
