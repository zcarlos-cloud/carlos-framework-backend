package com.carlos.org.manager;

/**
 * <p>
 * 短信验证码 Manager 接口
 * </p>
 *
 * <p>统一封装 Redis 缓存操作，验证码的读取与删除。</p>
 *
 * @author Carlos
 * @date 2024/5/22 16:30
 */
public interface SmsCodeManager {

    /**
     * 获取短信验证码
     *
     * @param phone 手机号
     * @return 验证码值，不存在返回 null
     */
    String getSmsCode(String phone);

    /**
     * 删除短信验证码
     *
     * @param phone 手机号
     * @return 是否删除成功
     */
    boolean deleteSmsCode(String phone);
}
