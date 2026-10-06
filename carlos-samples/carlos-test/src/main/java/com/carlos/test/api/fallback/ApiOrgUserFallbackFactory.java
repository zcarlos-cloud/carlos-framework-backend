package com.carlos.test.api.fallback;

import com.carlos.test.api.ApiOrgUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;


/**
 * <p>
 * 系统用户 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2023-8-12 11:16:18
 */
@Slf4j
public class ApiOrgUserFallbackFactory implements FallbackFactory
        <ApiOrgUser> {

    @Override
    public ApiOrgUser create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("系统用户服务调用失败: message:{}", message);
        return new ApiOrgUser() {

        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgUserFallbackFactory userFallbackFactory() {
    //     return new ApiOrgUserFallbackFactory();
    // }
}
