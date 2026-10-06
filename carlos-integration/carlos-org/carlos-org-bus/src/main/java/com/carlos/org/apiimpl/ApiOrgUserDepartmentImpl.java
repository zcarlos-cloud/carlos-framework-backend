package com.carlos.org.apiimpl;


import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUserDepartment;
import com.carlos.org.api.pojo.ao.OrgUserDepartmentAO;
import com.carlos.org.convert.OrgUserDepartmentConvert;
import com.carlos.org.manager.OrgUserDepartmentManager;
import com.carlos.org.pojo.dto.OrgUserDepartmentDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 用户部门 api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/user/department")
@Tag(name = "用户部门Feign接口")
public class ApiOrgUserDepartmentImpl implements ApiOrgUserDepartment {

    private final OrgUserDepartmentManager userDepartmentManager;

    @Override
    public Result<List<OrgUserDepartmentAO>> listByUserId(@RequestParam("userId") Long userId) {
        List<OrgUserDepartmentDTO> dtoList = userDepartmentManager.listByUserId(userId);
        return Result.success(OrgUserDepartmentConvert.INSTANCE.toAOList(dtoList));
    }
}
