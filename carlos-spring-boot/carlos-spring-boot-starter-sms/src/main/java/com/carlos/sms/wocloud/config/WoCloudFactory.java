package com.carlos.sms.wocloud.config;

import com.carlos.sms.enums.SmsServiceType;
import com.carlos.sms.wocloud.core.WoCloud;
import com.carlos.sms.wocloud.core.WoCloudSmsImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.sms4j.provider.factory.AbstractProviderFactory;

/**
 * WoCloudSmsConfig
 * <p>邮政云短信建造对象
 *
 * @author Carlos 2023/4/8  15:46
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WoCloudFactory extends AbstractProviderFactory<WoCloudSmsImpl, WoCloudConfig> {

    private static final WoCloudFactory INSTANCE = new WoCloudFactory();

    /**
     * 获取建造者实例
     *
     * @return 建造者实例
     */
    public static WoCloudFactory instance() {
        return INSTANCE;
    }


    /**
     * 短信配置
     */
    private void buildSms(WoCloudConfig config) {
        WoCloud.init(config.getAccessKeyId(), config.getAccessKeySecret());
    }

    /**
     * 建造一个短信实现对像
     *
     * @param config 参数0
     * @author Carlos
     * @date 2023/11/21 1:26
     */
    @Override
    public WoCloudSmsImpl createSms(WoCloudConfig config) {
        this.buildSms(config);
        return new WoCloudSmsImpl(config);
    }

    /**
     * <p>
     * 获取供应商
     * </p>
     *
     * @author Carlos
     * @date 2023/11/21 1:26
     */
    @Override
    public String getSupplier() {
        return SmsServiceType.WOCLOUD.getCode();
    }

}
