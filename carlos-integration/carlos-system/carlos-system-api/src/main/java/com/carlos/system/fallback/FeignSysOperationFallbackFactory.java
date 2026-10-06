package com.carlos.system.fallback;

import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSysOperation;
import com.carlos.system.pojo.ao.SysOperationAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.List;

/**
 * <p>
 * 统一操作降级服务
 * </p>
 *
 * @author carlos
 * @date 2026-07-11
 */
@Slf4j
public class FeignSysOperationFallbackFactory implements FallbackFactory<ApiSysOperation> {

    @Override
    public ApiSysOperation create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("系统操作服务调用失败: message:{}", message);
        return new ApiSysOperation() {

            @Override
            public Result<List<SysOperationAO>> listEnabledOperations() {
                return Result.error();
            }
        };
    }
}
