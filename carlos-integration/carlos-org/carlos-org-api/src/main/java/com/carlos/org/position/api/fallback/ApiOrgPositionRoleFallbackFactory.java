package com.carlos.org.position.api.fallback;

import com.carlos.org.position.api.ApiOrgPositionRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
public class ApiOrgPositionRoleFallbackFactory implements FallbackFactory<ApiOrgPositionRole> {

    @Override
    public ApiOrgPositionRole create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("岗位角色关联表（岗位默认权限配置）服务调用失败: message:{}", message);
        return new ApiOrgPositionRole() {

        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgPositionRoleFallbackFactory positionRoleFallbackFactory() {
    //     return new ApiOrgPositionRoleFallbackFactory();
    // }
}