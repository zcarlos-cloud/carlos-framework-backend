package com.carlos.sms.postal.config;

import com.carlos.sms.enums.SmsServiceType;
import com.carlos.sms.postal.core.Postal;
import com.carlos.sms.postal.core.PostalSmsImpl;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.sms4j.provider.factory.AbstractProviderFactory;

/**
 * PostalSmsConfig
 * <p>邮政云短信建造对象
 *
 * @author Carlos 2023/4/8  15:46
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostalFactory extends AbstractProviderFactory<PostalSmsImpl, PostalConfig> {

    private static final PostalFactory INSTANCE = new PostalFactory();

    /**
     * 获取建造者实例
     *
     * @return 建造者实例
     */
    public static PostalFactory instance() {
        return INSTANCE;
    }


    /**
     * 短信配置
     */
    private void buildSms(PostalConfig postalConfig) {
        Postal.init(postalConfig.getAccessKeyId(), postalConfig.getAccessKeySecret());
    }

    /**
     * 建造一个短信实现对像
     *
     * @param postalConfig 参数0
     * @return com.carlos.sms.postal.core.PostalSmsImpl
     * @author Carlos
     * @date 2023/11/21 1:26
     */
    @Override
    public PostalSmsImpl createSms(PostalConfig postalConfig) {
        this.buildSms(postalConfig);
        return new PostalSmsImpl(postalConfig);
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
        return SmsServiceType.POSTAL.getCode();
    }

}
