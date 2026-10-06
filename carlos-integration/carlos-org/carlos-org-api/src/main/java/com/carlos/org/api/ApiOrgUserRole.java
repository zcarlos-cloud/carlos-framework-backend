package com.carlos.org.api;

import com.carlos.core.response.Result;
import com.carlos.org.ServiceNameConstant;
import com.carlos.org.api.fallback.ApiOrgUserRoleFallbackFactory;
import com.carlos.org.api.pojo.ao.OrgUserRoleAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>
 * 用户角色 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "userRole", path = "/api/org/user/role", fallbackFactory = ApiOrgUserRoleFallbackFactory.class)
public interface ApiOrgUserRole {

    /**
     * 根据用户ID查询角色关联列表
     *
     * @param userId 用户ID
     * @return 角色关联列表
     */
    @GetMapping("/listByUserId")
    Result<List<OrgUserRoleAO>> listByUserId(@RequestParam("userId") Long userId);

}
