package com.carlos.org.apiimpl;


import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUserRole;
import com.carlos.org.api.pojo.ao.OrgUserRoleAO;
import com.carlos.org.convert.OrgUserRoleConvert;
import com.carlos.org.manager.OrgUserRoleManager;
import com.carlos.org.pojo.dto.OrgUserRoleDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 用户角色 api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/user/role")
@Tag(name = "用户角色Feign接口")
public class ApiOrgUserRoleImpl implements ApiOrgUserRole {

    private final OrgUserRoleManager userRoleManager;

    @Override
    public Result<List<OrgUserRoleAO>> listByUserId(@RequestParam("userId") Long userId) {
        List<OrgUserRoleDTO> dtoList = userRoleManager.listByUserId(userId);
        return Result.success(OrgUserRoleConvert.INSTANCE.toAOList(dtoList));
    }
}
