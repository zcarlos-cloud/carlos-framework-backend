package com.carlos.org.position.api.fallback;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.position.api.ApiOrgPosition;
import com.carlos.org.position.api.pojo.ao.OrgPositionAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * <p>
 * 岗位表 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@Slf4j
public class ApiOrgPositionFallbackFactory implements FallbackFactory<ApiOrgPosition> {

    @Override
    public ApiOrgPosition create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("岗位表服务调用失败: message:{}", message);
        return new ApiOrgPosition() {

            @Override
            public Result<Paging<OrgPositionAO>> pagePositionOptions(String positionName, String positionCode,
                                                                     Integer pageNum, Integer pageSize) {
                log.error("岗位表服务调用失败(pagePositionOptions): positionName:{}, positionCode:{}, message:{}",
                    positionName, positionCode, message);
                Paging<OrgPositionAO> empty = new Paging<>();
                empty.setRecords(Collections.emptyList());
                return Result.success(empty);
            }

            @Override
            public Result<List<OrgPositionAO>> listPositionOptions(Collection<Long> ids) {
                log.error("岗位表服务调用失败(listPositionOptions): size:{}, message:{}",
                    ids == null ? 0 : ids.size(), message);
                return Result.success(Collections.emptyList());
            }
        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgPositionFallbackFactory positionFallbackFactory() {
    //     return new ApiOrgPositionFallbackFactory();
    // }
}
