package com.carlos.datascope;

import org.aspectj.lang.JoinPoint;

/**
 * <p>
 * 数据权限处理器
 * </p>
 *
 * @author Carlos
 * @date 2022/11/21 15:15
 */
public interface DataScopeHandler {


    /**
     * 处理注解内容
     *
     * @param scopes 注解
     * @author Carlos
     * @date 2022/11/21 15:39
     */
    void handle(DataScope[] scopes);

    /**
     * 处理器退出相关处理
     *
     * @author Carlos
     * @date 2022/11/21 15:39
     */
    void exit();

    /**
     * 分局方法索引获取数据权限信息
     *
     * @param methodPoint 方法坐标
     * @return com.carlos.datascope.DataScope
     * @author Carlos
     * @date 2022/11/22 15:09
     */
    DataScopeInfo getScopeInfo(String methodPoint);

    /**
     * 参数处理
     *
     * @param joinPoint 参数0
     * @author Carlos
     * @date 2023/1/28 16:55
     */
    void handleParam(JoinPoint joinPoint);
}
