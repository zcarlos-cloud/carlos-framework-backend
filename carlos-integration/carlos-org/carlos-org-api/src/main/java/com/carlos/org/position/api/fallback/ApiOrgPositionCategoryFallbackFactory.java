package com.carlos.org.position.api.fallback;

import com.carlos.org.position.api.ApiOrgPositionCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * <p>
 * 岗位类别表（职系） api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
public class ApiOrgPositionCategoryFallbackFactory implements FallbackFactory<ApiOrgPositionCategory> {

    @Override
    public ApiOrgPositionCategory create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("岗位类别表（职系）服务调用失败: message:{}", message);
        return new ApiOrgPositionCategory() {

        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgPositionCategoryFallbackFactory positionCategoryFallbackFactory() {
    //     return new ApiOrgPositionCategoryFallbackFactory();
    // }
}