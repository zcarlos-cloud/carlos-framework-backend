package com.carlos.org.apiimpl;


import cn.hutool.core.collection.CollUtil;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartment;
import com.carlos.org.api.pojo.ao.OrgDepartmentAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.convert.OrgDepartmentConvert;
import com.carlos.org.convert.OrgUserConvert;
import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.pojo.dto.OrgDepartmentDTO;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.entity.OrgDepartment;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 部门 api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/department")
@Tag(name = "部门Feign接口")
public class ApiOrgDepartmentImpl implements ApiOrgDepartment {

    private final OrgDepartmentManager departmentManager;

    @Override
    public Result<OrgDepartmentAO> getDepartmentById(@RequestParam("id") Long id) {
        OrgDepartmentDTO dto = departmentManager.getDtoById(id);
        return Result.success(OrgDepartmentConvert.INSTANCE.toAO(dto));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByDeptId(@RequestParam("deptId") Long deptId) {
        if (deptId == null) {
            return Result.success(List.of());
        }
        // 非递归：只返回直接归属该部门的用户，不含子部门
        List<OrgUserDTO> users = departmentManager.getUsersByDeptId(deptId);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByDeptIdAndTenant(@RequestParam("deptId") Long deptId,
                                                              @RequestParam("tenantId") Long tenantId) {
        if (deptId == null || tenantId == null) {
            return Result.success(List.of());
        }
        // 非递归：只返回直接归属该部门且属于指定租户的用户，不含子部门
        List<OrgUserDTO> users = departmentManager.getUsersByDeptIdAndTenant(deptId, tenantId);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByDeptIdRecursive(@RequestParam("deptId") Long deptId) {
        if (deptId == null) {
            return Result.success(List.of());
        }
        // 递归：部门集合（自身 + 全部子孙）由 Manager 在服务端展开，调用方只一次往返
        List<OrgUserDTO> users = departmentManager.getUsersByDeptIdRecursive(deptId);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<List<OrgUserAO>> listUsersByDeptIdRecursiveAndTenant(@RequestParam("deptId") Long deptId,
                                                                       @RequestParam("tenantId") Long tenantId) {
        if (deptId == null || tenantId == null) {
            return Result.success(List.of());
        }
        // 递归 + 指定租户：部门集合同样由 Manager 在服务端展开
        List<OrgUserDTO> users = departmentManager.getUsersByDeptIdRecursiveAndTenant(deptId, tenantId);
        return Result.success(OrgUserConvert.INSTANCE.toAOList(users));
    }

    @Override
    public Result<List<OrgDepartmentAO>> listAllDepartmentOptions() {
        // 部门量级在组织架构规模内：一次取回扁平列表，由调用方自行筛选 / 分页 / 建树
        return Result.success(OrgDepartmentConvert.INSTANCE.toAOList(departmentManager.listAll()));
    }

    @Override
    public Result<List<OrgDepartmentAO>> listDepartmentOptions(@RequestParam("ids") Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Result.success(List.of());
        }
        // 一次批量取回，避免调用方按 id 逐个回查（N+1）；顺序不保证，调用方按 id 建映射
        List<OrgDepartment> departments = departmentManager.listByIds(ids);
        return Result.success(OrgDepartmentConvert.INSTANCE.toAOList(departments.stream()
            .map(OrgDepartmentConvert.INSTANCE::toDTO)
            .toList()));
    }
}
