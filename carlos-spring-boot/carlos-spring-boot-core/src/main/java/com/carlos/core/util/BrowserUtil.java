package com.carlos.core.util;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 浏览器打开工具
 * </p>
 *
 * @author carlos
 * @date 2020/4/10 15:40
 */
@Slf4j
public class BrowserUtil {

    /**
     * 启动默认浏览器   ！！！请注意最后的空格
     */
    public static final String DEFAULT_BROWSER = "rundll32 url.dll,FileProtocolHandler ";

    /**
     * 使用默认浏览器打开链接
     *
     * @param url 需要打开得地址
     * @author carlos
     * @date 2020/7/20 16:04l
     */
    public static void execByBrowser(String url) {
        try {
            RuntimeUtil.exec(DEFAULT_BROWSER + url);
        } catch (Exception e) {
            log.error("浏览器访问：[{}]异常", url, e);
        }
    }
}
