package com.carlos.sms.ums.config;


import com.carlos.sms.enums.SmsServiceType;
import com.carlos.sms.ums.core.UmsSmsImpl;
import org.dromara.sms4j.provider.factory.BaseProviderFactory;

public class UmsFactory implements BaseProviderFactory<UmsSmsImpl, UmsConfig> {

    private static final UmsFactory INSTANCE = new UmsFactory();

    public static UmsFactory instance() {
        return INSTANCE;
    }

    @Override
    public UmsSmsImpl createSms(UmsConfig config) {
        return new UmsSmsImpl(config);
    }

    @Override
    public Class<UmsConfig> getConfigClass() {
        return UmsConfig.class;
    }

    @Override
    public String getSupplier() {
        return SmsServiceType.UMS.getCode();
    }
}
