package com.carlos.license.generate;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.license.CustomKeyStoreParam;
import com.carlos.license.CustomLicenseManager;
import com.carlos.license.LicenseCheckModel;
import com.carlos.license.exception.LicenseException;
import com.carlos.license.service.AbstractSystemInfoDao;
import de.schlichtherle.license.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.security.auth.x500.X500Principal;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.prefs.Preferences;

/**
 * <p>
 *   证书生成业务
 * # 1. 生成私钥库
 * # validity：私钥的有效期（天）
 * # alias：私钥别称
 * # keystore：私钥库文件名称（生成在当前目录）
 * # storepass：私钥库密码（获取 keystore 信息所需的密码，密钥库口令）
 * # keypass：别名条目的密码(密钥口令)
 * keytool -genkeypair -keysize 1024 -validity 3650 -alias "privateKey" -keystore "privateKeys.keystore" -storepass "pubwd123456" -keypass "priwd123456" -dname "CN=localhost, OU=localhost, O=localhost, L=SH, ST=SH, C=CN" (-dname "CN=名称, OU=单位部门, O=组织名称, L=城市, ST=州/省, C=国家代码")
 *
 * # 2. 把私钥库内的公钥导出到一个文件当中
 * # alias：私钥别称
 * # keystore：私钥库的名称（在当前目录查找）
 * # storepass：私钥库的密码
 * # file：证书名称
 * keytool -exportcert -alias "privateKey" -keystore "privateKeys.keystore" -storepass "pubwd123456" -file "certfile.cer"
 *
 * # 3.再把这个证书文件导入到公钥库，certfile.cer 没用了可以删掉了
 * # alias：公钥名称
 * # file：证书名称
 * # keystore：公钥文件名称
 * # storepass：公钥库密码
 * keytool -import -alias "publicCert" -file "certfile.cer" -keystore "publicCerts.keystore" -storepass "pubwd123456"
 * </p>
 *
 * @author Carlos
 * @date 2025-04-10 14:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseCreatorService {

    private final LicenseGenerateProperties properties;


    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: LicenseCreatorServiceImpl.java </p>
     * <p>方法描述: 获取服务器硬件信息 </p>
     * <p>创建时间: 2025/04/10 13:46 </p>
     *
     * @param osType 系统名称
     * @return com.carlos.license.LicenseCheckModel
     * @author Carlos
     * @version 1.0
     */
    public LicenseCheckModel getServerInfos(OSType osType) {
        //操作系统类型
        if (osType == null) {
            OsInfo osInfo = SystemUtil.getOsInfo();
            String osName = osInfo.getName();
            if (StrUtil.isBlank(osName)) {
                log.error("不支持的操作系统类型:name:{} arch:{}", osInfo.getName(), osInfo.getArch());
                throw CommonErrorCode.INTERNAL_ERROR.exception("系统信息读取失败");
            }
            osName = osName.toLowerCase();
            osType = OSType.tagOf(osName);

        }
        AbstractSystemInfoDao abstractSystemInfoDao = osType.getServerInfos();
        return abstractSystemInfoDao.getServerInfos();
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: LicenseCreatorServiceImpl.java </p>
     * <p>方法描述: 生成证书 </p>
     * <p>创建时间: 2025/04/10 13:46 </p>
     *
     * @param param 证书创建参数
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author Carlos
     * @version 1.0
     */
    public String generateLicense(LicenseCreatorParam param) {
        String fileId = IdUtil.fastUUID();
        LicenseCreatorParam.Identifier id = param.getIdentifier();
        String name = "CN=" + id.getAppName() + ", OU=" + id.getOrgUnit() + ", O=" + id.getOrg() + ", L=" + id.getCity() + ", ST=" + id.getProvince() + ", C=" + id.getCountry();
        String basePath = properties.getFilePath() + File.separator + fileId + File.separator;
        FileUtil.mkdir(basePath);
        String privateKeyPath = basePath + "privateKey.keystore";
        String publicCertPath = basePath + "publicCerts.cer";
        String publicCertKey = basePath + "publicCerts.keystore";
        String licensePath = basePath + "license.lic";
        generatePrivateKey(param, privateKeyPath, name);
        generateCertificates(param, privateKeyPath, publicCertPath);
        generatePublicKey(param, publicCertKey, publicCertPath);
        // 删除密钥库文件

        FileUtil.del(publicCertPath);

        Preferences preferences = Preferences.userNodeForPackage(LicenseCreator.class);

        //设置对证书内容加密的秘钥
        CipherParam cipherParam = new DefaultCipherParam(param.getStorePass());

        KeyStoreParam privateStoreParam = new CustomKeyStoreParam(LicenseCreator.class
                , privateKeyPath
                , param.getPrivateAlias()
                , param.getStorePass()
                , param.getKeyPass());

        DefaultLicenseParam licenseParam = new DefaultLicenseParam(param.getSubject()
                , preferences
                , privateStoreParam
                , cipherParam);

        LicenseManager licenseManager = new CustomLicenseManager(licenseParam);
        X500Principal issuer = new X500Principal(name);
        LicenseContent licenseContent = new LicenseContent();
        licenseContent.setHolder(issuer);
        licenseContent.setIssuer(issuer);
        licenseContent.setSubject(param.getSubject());
        licenseContent.setIssued(param.getIssuedTime());
        licenseContent.setNotBefore(param.getIssuedTime());
        licenseContent.setNotAfter(param.getExpiryTime());
        licenseContent.setConsumerType(param.getConsumerType());
        licenseContent.setConsumerAmount(param.getConsumerAmount());
        licenseContent.setInfo(param.getDescription());

        //扩展校验服务器硬件信息
        licenseContent.setExtra(param.getLicenseCheckModel());
        // 生成license文件
        try {
            licenseManager.store(licenseContent, new File(licensePath));
        } catch (Exception e) {
            log.error("证书生成失败：{}", e.getMessage(), e);
            throw new LicenseException(e);
        } finally {
            // 删除私钥库文件
            FileUtil.del(privateKeyPath);
        }
        return fileId;
    }


    /**
     * # 生成PKCS12格式的私钥库（替换原有风险操作）
     * keytool -genkeypair -alias privateKey \
     * -keyalg RSA -keysize 2048 \  # 改用RSA算法和2048位密钥
     * -storetype PKCS12 \          # 替换JKS为行业标准格式
     * -keystore /data/privateKeys.p12 \
     * -storepass pubwd123456 \
     * -keypass priwd123456 \
     * -validity 3650 \
     * -dname "CN=localhost, OU=localhost, O=localhost, L=SH, ST=SH, C=CN"
     *
     * @param param 参数0
     * @param path  参数1
     * @param name
     * @author Carlos
     * @date 2025-04-10 21:55
     */
    private void generatePrivateKey(LicenseCreatorParam param, String path, String name) {

        String[] command = {
                "keytool",
                "-genkeypair",
                "-alias", param.getPrivateAlias(),
                // "-keyalg", "RSA",
                "-keysize", "1024",
                // "-storetype", "PKCS12",
                "-validity", param.getValidity(),
                "-keystore", path,
                "-storepass", param.getStorePass(),
                "-keypass", param.getKeyPass(),
                "-dname", "\"" + name + "\""
        };

        Process process = RuntimeUtil.exec(command);
        log.info("执行命令:{} ", StrUtil.join(StrUtil.SPACE, command));
        // 等待命令执行完成
        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            log.error("命令执行失败：{}", e.getMessage(), e);
            throw new LicenseException("私钥生成命令执行失败！");
        }
        if (exitCode == 0) {  // 0表示成功
            String successMsg = RuntimeUtil.getResult(process);
            log.info("执行成功： {}", successMsg);
        } else {
            String errorMsg = RuntimeUtil.getErrorResult(process);   // 获取错误流信息
            String result = RuntimeUtil.getResult(process);   // 获取错误流信息
            log.info("执行失败：result:{}  error:{}", result, errorMsg);
            throw new LicenseException("私钥文件生成失败!");
        }
    }

    /**
     *# 导出证书
     * keytool -exportcert -alias privateKey \
     *   -keystore /data/privateKeys.p12 \
     *   -storepass pubwd123456 \
     *   -file /data/publicCert.cer
     *
     * @param param 参数0
     * @param privateKeyPath 私钥路径
     * @param publicCertPath 公钥路径
     * @author Carlos
     * @date 2025-04-10 21:55
     */
    private void generateCertificates(LicenseCreatorParam param, String privateKeyPath, String publicCertPath) {
// 构造命令参数数组（避免路径空格问题）
        String[] command = {
                "keytool",
                "-exportcert",
                "-alias", param.getPrivateAlias(),
                "-keystore", privateKeyPath,
                "-storepass", param.getStorePass(),
                "-file", publicCertPath
        };
        Process process = RuntimeUtil.exec(command);
        log.info("执行命令:{} ", StrUtil.join(StrUtil.SPACE, command));
        // 等待命令执行完成
        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            log.error("命令执行失败：{}", e.getMessage(), e);
            throw new LicenseException("certfile生成命令执行失败！");
        }
        if (exitCode == 0) {  // 0表示成功
            String successMsg = RuntimeUtil.getResult(process);
            log.info("执行成功： {}", successMsg);
        } else {
            String errorMsg = RuntimeUtil.getErrorResult(process);   // 获取错误流信息
            log.info("执行失败： {}", errorMsg);
            throw new LicenseException("certfile文件生成失败!");
        }
    }

    /**
     * # 导入公钥到客户端库（PKCS12兼容模式）
     * keytool -import \
     *   -alias publicCert \
     *   -file /data/publicCert.cer  \
     *   -keystore /data/publicCerts.p12 \
     *   -storetype PKCS12 \    # 统一使用PKCS12
     *   -storepass pubwd123456
     *
     * @param param 参数0
     * @param publicKeyPath 公钥地址
     * @param publicCertPath 公钥地址
     * @author Carlos
     * @date 2025-04-10 21:55
     */
    private void generatePublicKey(LicenseCreatorParam param, String publicKeyPath, String publicCertPath) {
// 构造命令参数数组（避免路径空格问题）
        String[] command = {
                "keytool",
                "-import",
                "-alias", param.getPublicAlias(),
                "-file", publicCertPath,
                "-keystore", publicKeyPath,
                // "-storetype", "PKCS12",
                "-storepass", param.getStorePass()
        };
        Process process = RuntimeUtil.exec(command);
        log.info("执行命令:{} ", StrUtil.join(StrUtil.SPACE, command));

        // 通过线程异步读取输出流（避免缓冲区满导致阻塞）
        Thread.startVirtualThread(() -> {
            String result = RuntimeUtil.getResult(process);
            log.info("Output:  {}", result);
        });

        // 向输入流写入确认指令（假设需要输入Y）
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
            writer.write("Y\n");  // \n表示回车
            writer.flush();
        } catch (IOException e) {
            log.error("向输入流写入确认指令失败：{}", e.getMessage(), e);
        }
        // 等待命令执行完成
        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            log.error("命令执行失败：{}", e.getMessage(), e);
            throw new LicenseException("certfile生成命令执行失败！");
        }
    }

    /**
     * 下载许可证文件
     *
     * @param id 参数0
     * @param response 参数1
     * @author Carlos
     * @date 2025-04-11 00:43
     */
    public void downloadLicense(String id, HttpServletResponse response) throws UnsupportedEncodingException {
        // 创建临时压缩文件
        File zipFile = ZipUtil.zip(properties.getFilePath() + File.separator + id);

        // 设置响应头（关键部分）
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(zipFile.getName(), "UTF-8"));
        response.setHeader("Content-Length", String.valueOf(zipFile.length()));

        try {
            JakartaServletUtil.write(response, zipFile);
        } catch (Exception e) {
            log.error("文件下载失败", e);
        } finally {
            FileUtil.del(zipFile);
        }
    }

}
