package com.carlos.openapi;


import com.carlos.core.auth.GrantTypeEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Swagger配置属性
 *
 * @author carlos
 * @date 2020/3/21
 **/
@Data
@ConfigurationProperties(prefix = "carlos.openapi")
public class OpenApiProperties {

    /**
     * 是否启用
     */
    private boolean enable = false;

    /**
     * 描述
     */
    private String description;

    /**
     * 标题
     */
    private String title;

    /**
     * 服务条款URL
     **/
    private String termsOfServiceUrl;

    /**
     * 版本
     */
    private String version;
    /**
     * 许可证
     **/
    private String license;

    /**
     * 许可证URL
     **/
    private String licenseUrl;

    /**
     * 接口联系人信息
     */
    private final ContactProperties contact = new ContactProperties();

    /**
     * oauth支持
     */
    private final Oauth2Properties auth2 = new Oauth2Properties();


    /**
     * 接口联系人信息
     */
    @Data
    public static class ContactProperties {

        /**
         * 联系人名字
         */
        private String name;

        /**
         * 联系人邮箱
         */
        private String email;

        /**
         * 联系人URL
         */
        private String url;
    }

    /**
     * oauth2开启
     */
    @Data
    public static class Oauth2Properties {

        /**
         * 联系人名字
         */
        private boolean enable = false;

        /**
         * token获取地址
         */
        private String tokenUrl;

        /**
         * 客户端id
         */
        private String clientId;

        /**
         * 授权模式
         */
        private GrantTypeEnum grantType;
    }

    /**
     * 自定义参数配置
     */
    private final List<ParameterConfig> parameters = new ArrayList<>();

    /**
     * 自定义参数配置， 可配置属性请参考{@link io.swagger.v3.oas.annotations.Parameter}
     */
    @Data
    public static class ParameterConfig {

        /**
         * 是否开启默认参数
         */
        private boolean enable = true;

        /**
         * 名称
         */
        private String name;

        /**
         * 描述
         */
        private String description;

        /**
         * 参数类型 header, cookie, body, query
         */
        private ParamTypeEnum type = ParamTypeEnum.header;

        /**
         * 数据类型
         */
        private String dataType = "String";

        /**
         * 是否必填
         */
        private boolean required;

        /**
         * 默认值
         */
        private String defaultValue;

        public enum ParamTypeEnum {
            header,
            cookie,
            body,
            query
        }
    }
}
