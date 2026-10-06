package com.carlos.org.apiimpl;


import cn.hutool.core.collection.CollUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgRole;
import com.carlos.org.api.pojo.ao.OrgRoleAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.convert.OrgRoleConvert;
import com.carlos.org.convert.OrgUserConvert;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.entity.OrgRole;
import com.carlos.org.pojo.param.OrgRolePageParam;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 系统角色 api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/role")
@Tag(name = "系统角色Feign接口")
public class ApiOrgRoleImpl implements ApiOrgRole {

    private final OrgRoleManager roleManager;
    private final OrgUserManager orgUserManager;

    @Override
    public Result<OrgRoleAO> getRoleById(@RequestParam("id") Long id) {
        OrgRoleDTO dto = roleManager.getDtoById(id);
        return Result.success(OrgRoleConvert.INSTANCE.toAO(dto));
    }

    @Override
    public Result<OrgRoleAO> getRoleByCode(@RequestParam("roleCode") String roleCode) {
        OrgRoleDTO dto = roleManager.getByRoleCode(roleCode);
        return Result.success(OrgRoleConvert.INSTANCE.toAO(dto));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByRoleId(@RequestParam("roleId") Long roleId) {
        if (roleId == null) {
            return Result.success(List.of());
        }
        // 先取角色下的用户ID集合，再批量查询用户，避免逐条查询
        List<Long> userIds = roleManager.listUserIdsByRoleId(roleId);
        List<OrgUserDTO> users = orgUserManager.listDtoByIds(userIds);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByRoleIdAndTenant(@RequestParam("roleId") Long roleId,
                                                              @RequestParam("tenantId") Long tenantId) {
        if (roleId == null || tenantId == null) {
            return Result.success(List.of());
        }
        // 先取指定租户内角色下的用户ID集合，再批量查询用户，避免逐条查询
        List<Long> userIds = roleManager.listUserIdsByRoleIdAndTenant(roleId, tenantId);
        List<OrgUserDTO> users = orgUserManager.listDtoByIdsAndTenant(userIds, tenantId);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<Paging<OrgRoleAO>> pageRoleOptions(@RequestParam("roleName") String roleName,
                                                     @RequestParam("roleCode") String roleCode,
                                                     @RequestParam("pageNum") Integer pageNum,
                                                     @RequestParam("pageSize") Integer pageSize) {
        OrgRolePageParam param = new OrgRolePageParam();
        param.setRoleName(roleName);
        param.setRoleCode(roleCode);
        // 0 是「未指定」的哨兵：ParamPage 的 setter 会把非正数归一成默认值（页码 1 / 每页 10）
        param.setCurrent(pageNum == null ? 0 : pageNum);
        param.setSize(pageSize == null ? 0 : pageSize);
        Paging<OrgRoleDTO> page = roleManager.getPage(param);
        return Result.success(new Paging<OrgRoleAO>()
            .setTotal(page.getTotal())
            .setCurrent(page.getCurrent())
            .setSize(page.getSize())
            .setPages(page.getPages())
            .setRecords(OrgRoleConvert.INSTANCE.toAOList(page.getRecords())));
    }

    @Override
    public Result<List<OrgRoleAO>> listRoleOptions(@RequestParam("ids") Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Result.success(List.of());
        }
        // 一次批量取回，避免调用方按 id 逐个回查（N+1）；顺序不保证，调用方按 id 建映射
        List<OrgRole> roles = roleManager.listByIds(ids);
        return Result.success(OrgRoleConvert.INSTANCE.toAOList(roles.stream()
            .map(OrgRoleConvert.INSTANCE::toDTO)
            .toList()));
    }
}
