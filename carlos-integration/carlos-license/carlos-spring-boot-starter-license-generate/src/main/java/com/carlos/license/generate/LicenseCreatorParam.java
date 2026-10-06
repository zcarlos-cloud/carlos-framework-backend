package com.carlos.license.generate;

import com.carlos.license.LicenseCheckModel;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 *   生成license参数
 * </p>
 *
 * @author Carlos
 * @date 2025-04-10 14:46
 */
@Data
public class LicenseCreatorParam implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 证书 subject
     */
    private String subject;
    /**
     * 密钥（私钥）别称
     */
    private String privateAlias;
    /**
     * 密钥（公钥）别称
     */
    private String publicAlias;
    /**
     * 私钥的有效期（天）
     */
    private String validity;
    /**
     * 密钥（私钥）密码（需要妥善保管，不能让使用者知道）
     */
    private String keyPass;

    /**
     * 访问秘钥库（私钥）的密码
     */
    private String storePass;
    /**
     * 证书生效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date issuedTime = new Date();

    /**
     * 证书失效时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expiryTime;
    /**
     * 用户类型
     */
    private String consumerType;
    /**
     * 用户数量
     */
    private Integer consumerAmount;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 额外的 License 校验参数
     */
    private LicenseCheckModel licenseCheckModel;

    /** 身份信息 */
    private Identifier identifier;

    /**
     * <p>
     *  身份信息
     * </p>
     *
     * @author Carlos
     * @date 2025-04-10 20:58
     */
    @Data
    public static class Identifier {
        /** CN (Common Name) 必填项，通用名称，通常填写域名、服务器名或应用名称（如：CN=www.example.com ） */
        private String appName;
        /** OU (Organizational Unit) 组织单位名称（如：OU=DevOps Team） */
        private String orgUnit;
        /** O (Organization)  组织名称（如：O=Example Corp） */
        private String org;
        /** L (Locality) 城市或区域名称（如：L=Shanghai） */
        private String city;
        /** ST (State/Province) 州或省份名称（如：ST=Shanghai） */
        private String province;
        /** C (Country Code) 必填项，国家两字母代码（如：C=CN代表中国） */
        private String country = "CN";
    }
}
