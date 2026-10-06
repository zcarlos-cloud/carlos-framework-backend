package com.carlos.core.spring;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * 应用相关信息工具
 * </p>
 *
 * @author carlos
 * @date 2019-05-08
 **/
@Slf4j
public class BootUtil {


    protected static String SERVER_IP;

    public BootUtil() {
        SERVER_IP = getServerIp();
    }

    /**
     * 获取当前服务的ip地址
     *
     * @author carlos
     * @date 2020/7/20 16:54
     */
    private static String getServerIp() {
        if (StringUtils.isBlank(SERVER_IP)) {
            // TODO: Carlos 2022/11/15 针对在本机环境，获取所有的网卡ip地址，防止部分地址无法访问
            return NetUtil.getLocalhostStr();
        }
        return SERVER_IP;
    }

    /**
     * 获取配置得值
     *
     * @param key 配置得名称  例如："server.port"
     * @author carlos
     * @date 2020/7/20 16:40
     */
    public static String getPropertyValue(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return SpringUtil.getProperty(key);
    }


    /**
     * 获取激活得配置文件
     *
     * @author carlos
     * @date 2020/7/20 17:04
     */
    public static String getProfileActive() {
        return getPropertyValue(BootConstant.PROFILES_ACTIVE);
    }

    /**
     * 获取首页地址
     *
     * @author carlos
     * @date 2020/7/20 17:04
     */
    public static String getHomeUrl() {
        return getAddress();
    }

    /**
     * 获取当前服务的端口号
     *
     * @author carlos
     * @date 2020/7/20 16:59
     */
    public static String getServerPort() {
        return getPropertyValue(BootConstant.SERVER_PORT);
    }

    /**
     * 获取当前服务content-path
     *
     * @author carlos
     * @date 2020/7/20 16:59
     */
    public static String getContextPath() {
        String contextPath = getPropertyValue(BootConstant.CONTEXT_PATH);
        return contextPath == null ? StrUtil.EMPTY : contextPath;
    }

    /**
     * 获取访问地址
     *
     * @return java.lang.String
     * @author carlos
     * @date 2021/12/7 17:28
     */
    public static String getAddress() {
        String baseUrl = "http://" + getServerIp() + StrUtil.COLON + getServerPort() + getContextPath();
        if (!StrUtil.endWith(baseUrl, StrUtil.SLASH)) {
            baseUrl = baseUrl + StrUtil.SLASH;
        }
        return baseUrl;
    }
}
