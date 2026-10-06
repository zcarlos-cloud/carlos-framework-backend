package com.carlos.auth.oauth2.codec;

import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.oauth2.repository.AuthorizationGrantTypeDeserializer;
import com.carlos.auth.oauth2.repository.AuthorizationGrantTypeMixin;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;

/**
 * OAuth2 授权信息编解码器
 *
 * <p>负责 {@link OAuth2Authorization} 的 JSON 序列化与反序列化。</p>
 *
 * <p><strong>修复历史：</strong></p>
 * <ul>
 *   <li>2026-05-23：修复 SecurityJackson2Modules allowlist 阻止反序列化问题</li>
 *   <li>2026-05-23：移除 Visibility.ANY，消除反序列化安全风险</li>
 *   <li>2026-05-23：自定义 Serializer/Deserializer 处理 private tokens Map</li>
 *   <li>2026-06-07：升级到 Jackson 3（Spring Boot 4.0.6）</li>
 *   <li>2026-09：注册 Spring Security 官方 Jackson 3 模块，修复 attributes 中的
 *       {@code Authentication}（如占位键 {@code java.security.Principal}）反序列化失败</li>
 * </ul>
 *
 * <p><strong>配置要点：</strong></p>
 * <ul>
 *   <li>Jackson 3 内置 JSR-310 日期时间支持，无需注册 JavaTimeModule</li>
 *   <li>通过 {@link SecurityJacksonModules} 注册 Spring Security 官方 Jackson 3 模块
 *       （{@code CoreJacksonModule}、{@code OAuth2AuthorizationServerJacksonModule} 等），
 *       由官方模块提供 Security/OAuth2 类型的 Mixin 与类型白名单</li>
 *   <li>default typing（{@code DefaultTyping.NON_FINAL} + {@code JsonTypeInfo.As.PROPERTY}）
 *       由官方模块统一激活，本类不再自行调用 {@code activateDefaultTyping}，
 *       避免两处配置互相覆盖（与 SAS {@code JdbcOAuth2AuthorizationService$Jackson3} 的写法一致）</li>
 *   <li>历史存量数据为 {@code As.WRAPPER_ARRAY} 格式。Jackson 的 {@code AsPropertyTypeDeserializer}
 *       兼容数组形式的类型信息，因此不含 {@code Authentication} 属性的旧数据仍可读取；
 *       但旧数据中的 {@code Authentication}（principal 被写成 {@code ["类名",{...}]} 数组）
 *       官方反序列化器无法识别（它只处理对象形式的 principal），读不出来，
 *       需要用户重新登录以生成新格式的授权信息（修复前同样读不出来）</li>
 *   <li>添加 {@link AuthorizationGrantType} Mixin 处理</li>
 *   <li>{@link BasicPolymorphicTypeValidator} 白名单 = 官方模块条目 + carlos 自有条目</li>
 *   <li>自定义 {@link OAuth2AuthorizationSerializer}/{@link OAuth2AuthorizationDeserializer}
 *       最后注册，确保覆盖官方模块对 {@link OAuth2Authorization} 的注册</li>
 * </ul>
 *
 * @author Carlos
 * @date 2026-05-19
 */
@Slf4j
public class OAuth2AuthorizationCodec {

    private final ObjectMapper objectMapper;

    public OAuth2AuthorizationCodec() {
        this.objectMapper = createObjectMapper();
    }

    /**
     * 配置 ObjectMapper（Jackson 3 builder 模式）
     */
    private ObjectMapper createObjectMapper() {
        ClassLoader classLoader = OAuth2AuthorizationCodec.class.getClassLoader();

        // 1. 配置白名单：先放入 carlos 自有条目，官方模块随后会把其安全类型写入同一个 builder。
        //    注意：白名单按最小权限原则配置，仅允许 attributes 中实际可能出现的类型
        var ptvBuilder = BasicPolymorphicTypeValidator.builder()
            .allowIfBaseType("java.lang")
            .allowIfBaseType("java.util")
            .allowIfBaseType("java.time")
            .allowIfBaseType("java.math")
            .allowIfBaseType("org.springframework.security.oauth2.core")
            .allowIfBaseType("org.springframework.security.oauth2.server.authorization")
            .allowIfBaseType("org.springframework.security.core")
            .allowIfBaseType("org.springframework.security.authentication");

        // 2. 加载 Spring Security 官方 Jackson 3 模块（Security 7 起提供，包名 org.springframework.security.jackson）。
        //    该列表末尾会被追加一个内部模块，由它激活 default typing：
        //    activateDefaultTyping(ptvBuilder.build(), NON_FINAL, As.PROPERTY)，
        //    白名单取的就是上面这个 ptvBuilder（含 carlos 自有条目 + 官方模块写入的条目）。
        //    因此这里不再自行调用 activateDefaultTyping：两处同时设置时，生效方取决于模块 setup
        //    与 builder 配置的执行顺序（模块后执行，会覆盖 builder 的配置），属于隐式覆盖。
        //    统一由官方模块激活也是 SAS JdbcOAuth2AuthorizationService$Jackson3 的写法；
        //    若官方将来移除该模块，会因失去类型信息导致 principal 用例失败，可及时发现。
        List<JacksonModule> modules = SecurityJacksonModules.getModules(classLoader, ptvBuilder);

        // 3. 自定义模块最后加入（最后 setup），确保覆盖官方模块对 OAuth2Authorization 的注册
        var customModule = new SimpleModule();
        customModule.addSerializer(OAuth2Authorization.class, new OAuth2AuthorizationSerializer());
        customModule.addDeserializer(OAuth2Authorization.class, new OAuth2AuthorizationDeserializer());
        customModule.addDeserializer(AuthorizationGrantType.class, new AuthorizationGrantTypeDeserializer());
        modules.add(customModule);

        // 4. 使用 JsonMapper.builder() 构建不可变的 ObjectMapper（Jackson 3）
        return JsonMapper.builder()
            .addModules(modules)
            .addMixIn(AuthorizationGrantType.class, AuthorizationGrantTypeMixin.class)
            .addMixIn(OAuth2Authorization.class, OAuth2AuthorizationTypeMixin.class)
            .build();
    }

    /**
     * 禁用 OAuth2Authorization 的 default typing 类型信息包装
     * <p>由于使用自定义 Serializer/Deserializer，不需要 Jackson 自动添加类型信息</p>
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private static abstract class OAuth2AuthorizationTypeMixin {
    }

    /**
     * 序列化 OAuth2Authorization
     *
     * @param authorization OAuth2 授权信息
     * @return JSON 字符串
     */
    public String serialize(OAuth2Authorization authorization) {
        try {
            return objectMapper.writeValueAsString(authorization);
        } catch (JacksonException e) {
            log.error("Failed to serialize OAuth2Authorization: id={}", authorization.getId(), e);
            throw AuthErrorCode.AUTH_OAUTH2_SERIALIZE_FAILED.exception(e.getMessage());
        }
    }

    /**
     * 反序列化 OAuth2Authorization
     *
     * @param json JSON 字符串
     * @return OAuth2 授权信息
     */
    public OAuth2Authorization deserialize(String json) {
        try {
            return objectMapper.readValue(json, OAuth2Authorization.class);
        } catch (JacksonException e) {
            log.error("Failed to deserialize OAuth2Authorization", e);
            throw AuthErrorCode.AUTH_OAUTH2_DESERIALIZE_FAILED.exception(e.getMessage());
        }
    }
}
