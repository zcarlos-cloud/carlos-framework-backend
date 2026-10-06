package com.carlos.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class WellKnownController {

    /**
     * 拦截 Chrome DevTools 自动发送的请求，避免日志打印 NoResourceFoundException
     */
    @RequestMapping("/.well-known/**")
    public ResponseEntity<Void> handleWellKnownRequests() {
        // 返回 404 或 204 都可以，204 表示无内容，更干净
        return ResponseEntity.notFound().build();
    }
}
