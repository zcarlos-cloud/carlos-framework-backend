package com.carlos.org.apiimpl;


import cn.hutool.core.util.StrUtil;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgTenant;
import com.carlos.org.api.pojo.ao.OrgTenantAO;
import com.carlos.org.convert.OrgTenantConvert;
import com.carlos.org.manager.OrgTenantManager;
import com.carlos.org.pojo.dto.OrgTenantDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 租户 api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/tenant")
@Tag(name = "租户Feign接口")
public class ApiOrgTenantImpl implements ApiOrgTenant {

    private final OrgTenantManager orgTenantManager;


    @Override
    public Result<OrgTenantAO> getByCode(String tenantCode) {
        if (StrUtil.isBlank(tenantCode)) {
            return Result.success(null);
        }
        OrgTenantDTO tenant = orgTenantManager.getByTenantCode(tenantCode);
        return Result.success(OrgTenantConvert.INSTANCE.toAO(tenant));
    }

}
