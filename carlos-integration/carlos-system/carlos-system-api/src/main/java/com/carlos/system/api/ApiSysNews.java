package com.carlos.system.api;

import com.carlos.core.response.Result;
import com.carlos.system.ServiceNameConstant;
import com.carlos.system.fallback.ApiSysNewsFallbackFactory;
import com.carlos.system.pojo.ao.SysNewsAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 公告/站内通知 feign 提供接口
 * </p>
 *
 * <p>S3 公告管理：供其他服务（如 carlos-message 推送集成）查询公告内容。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "news", fallbackFactory = ApiSysNewsFallbackFactory.class,
    path = "/api/sys/news")
public interface ApiSysNews {

    /**
     * 按ID查询公告
     *
     * @param id 公告ID
     * @return 公告对象，不存在返回 null data
     */
    @GetMapping("detail")
    Result<SysNewsAO> getById(@RequestParam("id") Long id);

}
