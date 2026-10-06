package com.carlos.auth.oauth2.client.config;

import lombok.Data;

/**
 * 客户端认证配置属性
 *
 * @author carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
// TODO: Carlos 2026-07-03 这个配置类需要设计和使用完善
@Data
public class ClientAuthProperties {

    /**
     * 是否启用数据库客户端仓库
     */
    private boolean dbRepository = false;

    /**
     * 是否启用自定义 Token 生成器
     */
    private boolean customTokenGenerator = false;

    /**
     * 是否启用客户端 Token 增强
     */
    private boolean tokenCustomizerEnabled = true;

    /**
     * 是否支持 POST Body 方式提交客户端凭证
     */
    private boolean postBodyAuthEnabled = true;

}
