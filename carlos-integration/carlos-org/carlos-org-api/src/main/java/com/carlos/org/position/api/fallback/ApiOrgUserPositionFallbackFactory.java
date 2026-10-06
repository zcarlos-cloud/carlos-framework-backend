package com.carlos.org.position.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.position.api.ApiOrgUserPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;


/**
 * <p>
 * 用户岗位职级关联表（核心任职信息） api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
public class ApiOrgUserPositionFallbackFactory implements FallbackFactory<ApiOrgUserPosition> {

    @Override
    public ApiOrgUserPosition create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("用户岗位职级关联表（核心任职信息）服务调用失败: message:{}", message);
        return new ApiOrgUserPosition() {

            @Override
            public Result<List<OrgUserAO>> listSubordinatesByUserId(Long userId) {
                log.error("用户岗位职级关联表（核心任职信息）服务调用失败(listSubordinatesByUserId): userId:{}, message:{}",
                    userId, message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<OrgUserAO>> listSubordinatesByUserIdAndTenant(Long userId, Long tenantId) {
                log.error("用户岗位职级关联表（核心任职信息）服务调用失败(listSubordinatesByUserIdAndTenant): userId:{}, tenantId:{}, message:{}",
                    userId, tenantId, message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<OrgUserAO> getSuperiorByUserId(Long userId) {
                log.error("用户岗位职级关联表（核心任职信息）服务调用失败(getSuperiorByUserId): userId:{}, message:{}",
                    userId, message);
                return Result.error("用户岗位职级关联表（核心任职信息）服务调用失败");
            }

            @Override
            public Result<OrgUserAO> getSuperiorByUserIdAndTenant(Long userId, Long tenantId) {
                log.error("用户岗位职级关联表（核心任职信息）服务调用失败(getSuperiorByUserIdAndTenant): userId:{}, tenantId:{}, message:{}",
                    userId, tenantId, message);
                return Result.error("用户岗位职级关联表（核心任职信息）服务调用失败");
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByPositionId(Long positionId) {
                log.error("用户岗位职级关联表（核心任职信息）服务调用失败(listUsersByPositionId): positionId:{}, message:{}",
                    positionId, message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByPositionIdAndTenant(Long positionId, Long tenantId) {
                log.error("用户岗位职级关联表（核心任职信息）服务调用失败(listUsersByPositionIdAndTenant): positionId:{}, tenantId:{}, message:{}",
                    positionId, tenantId, message);
                return Result.success(Collections.emptyList());
            }
        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgUserPositionFallbackFactory userPositionFallbackFactory() {
    //     return new ApiOrgUserPositionFallbackFactory();
    // }
}