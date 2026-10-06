package com.carlos.org.api.fallback;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.api.pojo.ao.OrgRoleAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;



/**
 * <p>
 * 系统角色 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
public class ApiOrgRoleFallbackFactory implements FallbackFactory<ApiOrgRole> {

    @Override
    public ApiOrgRole create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("系统角色服务调用失败: message:{}", message);
        return new ApiOrgRole() {

            @Override
            public Result<OrgRoleAO> getRoleById(Long id) {
                return Result.success(null);
            }

            @Override
            public Result<OrgRoleAO> getRoleByCode(String roleCode) {
                return Result.success(null);
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByRoleId(Long roleId) {
                log.error("系统角色服务调用失败(listUsersByRoleId): roleId:{}, message:{}", roleId, message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByRoleIdAndTenant(Long roleId, Long tenantId) {
                log.error("系统角色服务调用失败(listUsersByRoleIdAndTenant): roleId:{}, tenantId:{}, message:{}",
                    roleId, tenantId, message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<Paging<OrgRoleAO>> pageRoleOptions(String roleName, String roleCode,
                                                             Integer pageNum, Integer pageSize) {
                log.error("系统角色服务调用失败(pageRoleOptions): roleName:{}, roleCode:{}, message:{}",
                    roleName, roleCode, message);
                Paging<OrgRoleAO> empty = new Paging<>();
                empty.setRecords(Collections.emptyList());
                return Result.success(empty);
            }

            @Override
            public Result<List<OrgRoleAO>> listRoleOptions(Collection<Long> ids) {
                log.error("系统角色服务调用失败(listRoleOptions): size:{}, message:{}",
                    ids == null ? 0 : ids.size(), message);
                return Result.success(Collections.emptyList());
            }
        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgRoleFallbackFactory roleFallbackFactory() {
    //     return new ApiOrgRoleFallbackFactory();
    // }
}