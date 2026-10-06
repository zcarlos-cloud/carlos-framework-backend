package com.carlos.org.position.apiimpl;


import com.carlos.org.position.api.ApiOrgPositionRole;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 岗位角色关联表（岗位默认权限配置） api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月3日 下午11:19:10
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/position/role")
@Tag(name = "岗位角色关联表（岗位默认权限配置）Feign接口")
public class ApiOrgPositionRoleImpl implements ApiOrgPositionRole {


}
