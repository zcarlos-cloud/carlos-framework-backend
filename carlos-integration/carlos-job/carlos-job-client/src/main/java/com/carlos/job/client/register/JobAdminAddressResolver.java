package com.carlos.job.client.register;

import cn.hutool.core.util.StrUtil;
import com.carlos.job.client.config.JobClientProperties;

/**
 * <p>
 * 调度中心地址解析器
 * </p>
 *
 * <p>当前只支持显式配置 {@code carlos.job.client.admin-url}（零额外依赖，boot 单体与 cloud 微服务通用）。
 * 若后续要支持按服务名从 Nacos 发现 carlos-job，扩展点在这里：新增一个
 * {@code @ConditionalOnClass(LoadBalancerClient)} 守卫的<b>嵌套配置类</b>来替换本 Bean，
 * 且 {@code LoadBalancerClient} 不得出现在任何非嵌套类的字节码中，否则 boot 单体应用会
 * {@code NoClassDefFoundError}。</p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
public class JobAdminAddressResolver {

    private final JobClientProperties clientProperties;

    public JobAdminAddressResolver(JobClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    /**
     * 解析调度中心基础地址
     *
     * @return 形如 {@code http://host:port}（已去掉结尾斜杠），未配置返回 null
     */
    public String resolve() {
        String adminUrl = clientProperties.getAdminUrl();
        if (StrUtil.isBlank(adminUrl)) {
            return null;
        }
        return StrUtil.removeSuffix(adminUrl.trim(), "/");
    }

}
