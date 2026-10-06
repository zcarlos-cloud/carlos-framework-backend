package com.carlos.org.config;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * 蓉政通属性配置
 * </p>
 *
 * @author Carlos
 * @date 2023/4/6 9:34
 */
@Slf4j
@Data
@ConfigurationProperties(prefix = "carlos.org")
public class OrgProperties implements InitializingBean {


    private String defaultPassword;

    /**
     * 密码过期时间
     */
    private Duration passwordExpire = Duration.ofDays(90);

    /**
     * 登录属性
     */
    private OrgLoginProperties login = new OrgLoginProperties();


    /**
     * 初始化属性
     */
    @NestedConfigurationProperty
    private OrgInitProperties init = new OrgInitProperties();

    /**
     * 租户隔离属性
     */
    @NestedConfigurationProperty
    private OrgTenantProperties tenant = new OrgTenantProperties();

    /**
     * 登录属性
     */
    @Data
    public static class OrgLoginProperties {

        /**
         * 计数最大间隔时间  单位：分钟
         */
        private Long countIntervalTime = 24 * 60L;

        /**
         * 登录失败限制次数  单位：次
         */
        private Long failLimit = 5L;

        /**
         * 锁定时长 单位：分钟
         */
        private Long lockTime = 30L;

    }


    /**
     * 租户隔离属性
     *
     * <p>替代原 OrgTenantLineHandler 硬编码的 TENANT_TABLES；
     * 新增租户表只需追加配置 carlos.org.tenant.tables，无需改代码。</p>
     */
    @Data
    public static class OrgTenantProperties {

        /**
         * 需要租户隔离的表（只拦截真实存在 tenant_id 列的表；
         * org_tenant 自身与 org_user_department 无该列，不得加入）
         */
        private Set<String> tables = new HashSet<>(Arrays.asList(
            "org_user",
            "org_department",
            "org_role",
            "org_position",
            "org_position_category",
            "org_position_level",
            "org_position_role",
            "org_position_history",
            "org_user_role",
            "org_user_position",
            "org_department_role",
            "org_role_operation"
        ));

        /**
         * 平台租户ID（无登录上下文时的默认租户；0 为保留的平台租户）
         */
        private Long platformTenantId = 0L;

        /**
         * 超级管理员是否跨租户放行（true 时超管查询不追加 tenant_id 条件，可见全部租户数据）
         *
         * <p>⚠️ 已知限制（代码评审 H1）：MyBatis-Plus 租户插件对 INSERT 同样先判 ignoreTable，
         * 开启本项后超管写入租户表时 tenant_id 不会被自动填充（实体未显式赋值则为 NULL，
         * 该数据对任何租户不可见）。开启时请确保各写入路径显式回填 tenantId
         * （参照 OrgUserService.addOrgUser 的做法），或仅在排查问题时临时开启。</p>
         */
        private Boolean superAdminIgnore = false;

    }


    @Override
    public void afterPropertiesSet() throws Exception {
        log.debug("org config:{}", JSONUtil.toJsonPrettyStr(this));
    }
}
