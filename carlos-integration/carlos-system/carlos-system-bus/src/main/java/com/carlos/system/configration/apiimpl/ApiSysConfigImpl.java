package com.carlos.system.configration.apiimpl;


import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSystemConfig;
import com.carlos.system.configration.convert.SysConfigConvert;
import com.carlos.system.configration.pojo.dto.SysConfigDTO;
import com.carlos.system.configration.service.SysConfigService;
import com.carlos.system.pojo.ao.SysConfigAO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * <p>
 * 系统配置 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:54
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/config")
@Tag(name = "系统配置")
public class ApiSysConfigImpl implements ApiSystemConfig {


    private final SysConfigService configService;

    @Override
    @GetMapping("code")
    @Operation(summary = "根据code获取配置")
    public Result<SysConfigAO> getByCode(String code) {
        SysConfigDTO config = this.configService.getByCode(code);
        SysConfigAO ao = SysConfigConvert.INSTANCE.toAO(config);
        return Result.success(ao);
    }
}
