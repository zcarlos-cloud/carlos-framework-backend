package com.carlos.core.auth;

import cn.hutool.core.bean.copier.BeanCopier;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.MultiValueMap;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * Oauth2获取令牌参数
 * </p>
 *
 * @author carlos
 * @date 2021/11/4 15:26
 */
@Data
@Builder
public class AccessTokenParam implements Serializable {

    @Schema(description = "授权方式", requiredMode = Schema.RequiredMode.REQUIRED)
    private String grantType;

    @Schema(description = "客户端id", requiredMode = Schema.RequiredMode.REQUIRED)
    private String clientId;

    @Schema(description = "Oauth2客户端秘钥", requiredMode = Schema.RequiredMode.REQUIRED)
    private String clientSecret;

    @Schema(description = "刷新token")
    private String refreshToken;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "scope")
    private String scope;

    @Schema(description = "登录密码")
    private String password;

    @Schema(description = "请求头细心")
    private MultiValueMap<String, String> headers;

    /**
     * 将对象转换成map
     *
     * @return java.util.Map<java.lang.String, java.lang.String>
     * @author carlos
     * @date 2021/11/4 15:47
     */
    public Map<String, String> toMap() {
        return BeanCopier.create(this, new LinkedHashMap<String, String>(),
            CopyOptions.create()
                .setIgnoreNullValue(true)
                .setFieldNameEditor(StrUtil::toUnderlineCase)
        ).copy();
    }
}

