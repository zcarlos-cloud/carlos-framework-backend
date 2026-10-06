package com.carlos.system.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * <p>
 *   启动完成事件监听
 * </p>
 *
 * @author Carlos
 * @date 2025-12-05 13:57
 */
@Component
class SystemEventListener {

    /**
     * 监听 ApplicationReadyEvent
     * @param event
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady(ApplicationReadyEvent event) {


    }
}
