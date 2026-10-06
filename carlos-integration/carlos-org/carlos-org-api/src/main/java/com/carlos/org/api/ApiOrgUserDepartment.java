package com.carlos.org.api;

import com.carlos.core.response.Result;
import com.carlos.org.ServiceNameConstant;
import com.carlos.org.api.fallback.ApiOrgUserDepartmentFallbackFactory;
import com.carlos.org.api.pojo.ao.OrgUserDepartmentAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>
 * 用户部门 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "userDepartment", path = "/api/org/user/department", fallbackFactory = ApiOrgUserDepartmentFallbackFactory.class)
public interface ApiOrgUserDepartment {

    /**
     * 根据用户ID查询部门关联列表
     *
     * @param userId 用户ID
     * @return 部门关联列表
     */
    @GetMapping("/listByUserId")
    Result<List<OrgUserDepartmentAO>> listByUserId(@RequestParam("userId") Long userId);

}
