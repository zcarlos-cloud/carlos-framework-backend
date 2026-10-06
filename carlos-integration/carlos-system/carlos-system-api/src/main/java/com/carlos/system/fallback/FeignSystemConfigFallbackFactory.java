package com.carlos.system.fallback;

import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSystemConfig;
import com.carlos.system.pojo.ao.SysConfigAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * <p>
 * 用户降级服务
 * </p>
 *
 * @author Carlos
 * @date 2022/11/16 10:57
 */

@Slf4j
public class FeignSystemConfigFallbackFactory implements FallbackFactory<ApiSystemConfig> {

    @Override
    public ApiSystemConfig create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("用户服务调用失败: message:{}", message);
        return new ApiSystemConfig() {


            @Override
            public Result<SysConfigAO> getByCode(String code) {
                return Result.error("系统配置读取失败");
            }
        };
    }
}
