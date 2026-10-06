package com.carlos.license.generate;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.exception.RestException;
import com.carlos.license.LicenseCheckModel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author Carlos
 */
@RestController
@RequestMapping("/license")
@RequiredArgsConstructor
public class LicenseCreatorController {

    private final LicenseCreatorService licenseCreatorService;

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: LicenseCreatorController.java </p>
     * <p>方法描述: 获取服务器硬件信息 </p>
     * <p>创建时间: 2025/04/10 13:39 </p>
     *
     * @param osType 系统类型
     * @return com.carlos.license.LicenseCheckModel
     * @author Carlos
     * @version 1.0
     */
    @GetMapping("/getServerInfos")
    // TODO: Carlos 2025-04-10 该接口的功能应该用一个脚本或者什么代替，方便客户快速获取信息，提交给证书下放者
    public LicenseCheckModel getServerInfos(OSType osType) {
        return licenseCreatorService.getServerInfos(osType);
    }


    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: LicenseCreatorController.java </p>
     * <p>方法描述: 生成证书 </p>
     * <p>创建时间: 2025/04/10 13:42 </p>
     *
     * @param param 证书创建参数
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author Carlos
     * @version 1.0
     */
    @PostMapping("/generate")
    public String generateLicense(@RequestBody LicenseCreatorParam param) {
        return licenseCreatorService.generateLicense(param);
    }

    /**
     * 下载许可证文件
     *
     * @author Carlos
     * @date 2025-04-11 00:44
     */
    @GetMapping("/download")
    public void downloadLicense(String id, HttpServletResponse response) throws Throwable {
        if (StrUtil.isBlank(id)) {
            throw new RestException("id不能为空！");
        }
        licenseCreatorService.downloadLicense(id, response);
    }
}
