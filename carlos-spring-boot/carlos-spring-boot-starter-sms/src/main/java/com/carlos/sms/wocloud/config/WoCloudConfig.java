package com.carlos.sms.wocloud.config;

import com.carlos.sms.enums.SmsServiceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.sms4j.provider.config.BaseConfig;

/**
 * <p>
 * 邮政云短信配置
 * <a href="https://dx.11185.cn/isms-doc/?t=0.8030975974852081">接口文档地址</a>
 * </p>
 *
 * @author Carlos
 * @date 2023/11/21 1:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WoCloudConfig extends BaseConfig {

    /**
     * 获取供应商
     *
     * @since 3.0.0
     */
    @Override
    public String getSupplier() {
        return SmsServiceType.WOCLOUD.getCode();
    }

}
