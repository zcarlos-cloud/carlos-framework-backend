package com.carlos.sms.ums.config;


import com.carlos.sms.enums.SmsServiceType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.sms4j.provider.config.BaseConfig;

/**
 * <p>
 *   宜宾一信通短信
 * </p>
 *
 * @author Carlos
 * @date 2025-03-26 10:57 
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UmsConfig extends BaseConfig {

    /** 企业编号 */
    private String endpoint = "https://api.ums86.com:9600";

    /** 企业编号 */
    private String spCode;


    @Override
    public String getSupplier() {
        return SmsServiceType.UMS.getCode();
    }

}
