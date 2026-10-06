package com.carlos.org.api.fallback;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.api.pojo.ao.OrgUserCandidateAO;
import com.carlos.org.api.pojo.param.ApiOrgUserChangeStateParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.List;


/**
 * <p>
 * 系统用户 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
public class ApiOrgUserFallbackFactory implements FallbackFactory<ApiOrgUser> {

    @Override
    public ApiOrgUser create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("系统用户服务调用失败: message:{}", message);
        return new ApiOrgUser() {

            @Override
            public Result<OrgUserAO> getUserByIdentifier(String identifier) {
                log.error("系统用户服务调用失败: message:{}", message);
                return Result.error("系统用户服务调用失败");
            }

            @Override
            public Result<OrgUserAO> getUserByIdentifierAndTenant(String identifier, Long tenantId) {
                log.error("系统用户服务调用失败(getUserByIdentifierAndTenant): tenantId:{}, message:{}", tenantId, message);
                return Result.error("系统用户服务调用失败");
            }

            @Override
            public Result<OrgUserAO> getUserById(Long id) {
                log.error("系统用户服务调用失败(getUserById): id:{}, message:{}", id, message);
                return Result.error("系统用户服务调用失败");
            }

            @Override
            public Result<OrgUserAO> getUserByIdAndTenant(Long id, Long tenantId) {
                log.error("系统用户服务调用失败(getUserByIdAndTenant): id:{}, tenantId:{}, message:{}",
                    id, tenantId, message);
                return Result.error("系统用户服务调用失败");
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByIds(Collection<Long> ids) {
                log.error("系统用户服务调用失败(listUsersByIds): size:{}, message:{}", ids == null ? 0 : ids.size(), message);
                return Result.error("系统用户服务调用失败");
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByIdsAndTenant(Collection<Long> ids, Long tenantId) {
                log.error("系统用户服务调用失败(listUsersByIdsAndTenant): size:{}, tenantId:{}, message:{}",
                    ids == null ? 0 : ids.size(), tenantId, message);
                return Result.error("系统用户服务调用失败");
            }

            @Override
            public Result<Paging<OrgUserCandidateAO>> pageCandidates(Long deptId, Collection<Long> includeIds,
                                                                     Collection<Long> excludeIds, int current, int size) {
                log.error("系统用户服务调用失败(pageCandidates): deptId:{}, message:{}", deptId, message);
                return Result.error("系统用户服务调用失败");
            }

            @Override
            public Result<Paging<OrgUserAO>> pageUserOptions(String account, String nickname,
                                                             Integer pageNum, Integer pageSize) {
                log.error("系统用户服务调用失败(pageUserOptions): account:{}, nickname:{}, message:{}",
                    account, nickname, message);
                return Result.error("系统用户服务调用失败");
            }

            @Override
            public void changeState(ApiOrgUserChangeStateParam param) {
                log.error("系统用户服务调用失败(changeState): message:{}", message);
            }

            @Override
            public Result<Void> updatePassword(Long userId, String encodedPassword) {
                log.error("系统用户服务调用失败(updatePassword): userId:{}, message:{}", userId, message);
                return Result.error("系统用户服务调用失败");
            }
        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgUserFallbackFactory userFallbackFactory() {
    //     return new ApiOrgUserFallbackFactory();
    // }
}
