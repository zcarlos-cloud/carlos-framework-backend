package com.carlos.system.fallback;

import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSequence;
import com.carlos.system.pojo.ao.SequenceBatchAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 序列号服务熔断降级工厂
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Component
public class ApiSequenceFallbackFactory implements FallbackFactory<ApiSequence> {

    @Override
    public ApiSequence create(Throwable cause) {
        log.error("ApiSequence 调用失败: {}", cause.getMessage());
        return new ApiSequence() {
            @Override
            public Result<String> next(String ruleCode) {
                log.error("ApiSequence.next 降级, ruleCode={}, cause={}", ruleCode, cause.getMessage());
                return Result.error("序列号服务不可用");
            }

            @Override
            public Result<SequenceBatchAO> nextBatch(String ruleCode, int count) {
                log.error("ApiSequence.nextBatch 降级, ruleCode={}, count={}, cause={}", ruleCode, count,
                    cause.getMessage());
                return Result.error("序列号服务不可用");
            }
        };
    }

}
