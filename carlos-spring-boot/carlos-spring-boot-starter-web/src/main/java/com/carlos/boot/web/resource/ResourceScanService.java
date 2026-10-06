package com.carlos.boot.web.resource;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.boot.web.config.ResourceScanProperties;
import com.carlos.boot.web.resource.model.ApiResource;
import com.carlos.boot.web.resource.model.ResourceApp;
import com.carlos.boot.web.resource.model.ResourceGroup;
import com.carlos.core.exception.ComponentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 资源扫描服务，使用 Spring 内置 {@link ClassPathScanningCandidateComponentProvider} 扫描
 * Controller 端点，收集 API 资源元数据。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceScanService {

    private final ResourceScanProperties properties;
    private final ResourceStore resourceStore;

    private static final Map<Class<? extends Annotation>, RequestMethod[]> METHOD_ANNOTATIONS = new LinkedHashMap<>();

    /** 从 {@code hasPermission('res', 'act')} 表达式里提取权限码 */
    private static final Pattern PERM_PATTERN =
        Pattern.compile("hasPermission\\(\\s*'([^']+)'\\s*,\\s*'([^']+)'\\s*\\)");

    /** @PreAuthorize 的全限定名（反射按名读取，避免 starter-web 反向依赖 spring-security） */
    private static final String PRE_AUTHORIZE_FQN =
        "org.springframework.security.access.prepost.PreAuthorize";

    static {
        METHOD_ANNOTATIONS.put(RequestMapping.class, null);
        METHOD_ANNOTATIONS.put(GetMapping.class, new RequestMethod[]{RequestMethod.GET});
        METHOD_ANNOTATIONS.put(PostMapping.class, new RequestMethod[]{RequestMethod.POST});
        METHOD_ANNOTATIONS.put(PutMapping.class, new RequestMethod[]{RequestMethod.PUT});
        METHOD_ANNOTATIONS.put(DeleteMapping.class, new RequestMethod[]{RequestMethod.DELETE});
    }

    /**
     * 扫描并收集所有 API 资源，保存到 ResourceStore。
     */
    public ResourceApp scan() {
        try {
            Set<String> packages = properties.getScanPackages();
            if (CollectionUtil.isEmpty(packages)) {
                throw new ComponentException("ResourceScan: 'scanPackages' is empty");
            }

            ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

            List<ResourceGroup> groups = new LinkedList<>();

            for (String pkg : packages) {
                for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                    try {
                        Class<?> clazz = Class.forName(bd.getBeanClassName());
                        if (!clazz.isAnnotationPresent(Tag.class)) {
                            continue;
                        }
                        ResourceGroup group = processController(clazz);
                        groups.add(group);
                    } catch (Exception e) {
                        log.warn("ResourceScan: failed to process {}", bd.getBeanClassName(), e);
                    }
                }
            }

            ResourceApp app = new ResourceApp();
            app.setAppName(properties.getScanPackages().toString());
            app.setGroups(groups);
            resourceStore.save(app);

            log.info("ResourceScan: collected {} resource groups", groups.size());
            return app;
        } catch (Exception e) {
            log.error("ResourceScan: failed", e);
            throw new ComponentException("初始化资源异常");
        }
    }

    private ResourceGroup processController(Class<?> clazz) {
        Tag tag = clazz.getAnnotation(Tag.class);
        String groupName = tag != null && StrUtil.isNotBlank(tag.name()) ? tag.name() : clazz.getSimpleName();

        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        String[] prefixArray = (requestMapping != null && ArrayUtil.isNotEmpty(requestMapping.value()))
            ? requestMapping.value()
            : (requestMapping != null && ArrayUtil.isNotEmpty(requestMapping.path())
            ? requestMapping.path()
            : new String[]{"/"});

        List<Method> methods = ReflectUtil.getPublicMethods(clazz,
            method -> method.isAnnotationPresent(Operation.class));

        List<ApiResource> resources = new LinkedList<>();
        for (String prefix : prefixArray) {
            prefix = StrUtil.addPrefixIfNot(prefix, "/");
            for (Method method : methods) {
                processMethod(method, prefix, resources);
            }
        }

        ResourceGroup group = new ResourceGroup();
        group.setName(groupName);
        group.setResources(resources);
        return group;
    }

    private void processMethod(Method method, String prefix, List<ApiResource> resources) {
        Operation operation = method.getAnnotation(Operation.class);
        String resourceName = operation != null ? operation.summary() : method.getName();
        boolean hidden = operation != null && operation.hidden();

        for (Map.Entry<Class<? extends Annotation>, RequestMethod[]> entry : METHOD_ANNOTATIONS.entrySet()) {
            Annotation annotation = method.getAnnotation(entry.getKey());
            if (annotation == null) {
                continue;
            }

            String[] paths = resolvePaths(annotation);
            if (paths.length == 0) {
                paths = new String[]{""};
            }
            RequestMethod[] requestMethods = entry.getValue();
            if (requestMethods == null) {
                requestMethods = ((RequestMapping) annotation).method();
            }

            for (String path : paths) {
                path = path.isEmpty() ? path : StrUtil.addPrefixIfNot(path, "/");
                path = ReUtil.replaceAll(path, "\\{.*?\\}", "**");
                String fullPath = prefix + path;

                for (RequestMethod requestMethod : requestMethods) {
                    ApiResource resource = new ApiResource();
                    resource.setName(resourceName);
                    resource.setPathPrefix(StrUtil.addPrefixIfNot(properties.getPathPrefix(), "/"));
                    resource.setPath(fullPath);
                    resource.setMethod(requestMethod);
                    resource.setHidden(hidden);
                    resource.setDescription(method.getDeclaringClass().getSimpleName() + "#" + method.getName());
                    resource.setPermCode(resolvePermCode(method));
                    resources.add(resource);
                }
            }
            return;
        }
    }

    /**
     * 从方法上的 {@code @PreAuthorize("hasPermission('res','act')")} 提取权限码 {@code res:act}。
     *
     * <p>用反射按类名读取注解，避免 starter-web 反向依赖 spring-security。
     * 方法未声明该方法权限时返回 null，由存储端按路径约定兜底。</p>
     *
     * @param method 控制器方法
     * @return 权限码，未声明时返回 null
     */
    private String resolvePermCode(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (!PRE_AUTHORIZE_FQN.equals(annotation.annotationType().getName())) {
                continue;
            }
            try {
                Object value = annotation.annotationType().getMethod("value").invoke(annotation);
                if (value instanceof String[] arr && arr.length > 0) {
                    Matcher matcher = PERM_PATTERN.matcher(arr[0]);
                    if (matcher.find()) {
                        return matcher.group(1) + ":" + matcher.group(2);
                    }
                }
            } catch (Exception e) {
                log.debug("ResourceScan: failed to read @PreAuthorize on {}", method.getName(), e);
            }
        }
        return null;
    }

    private String[] resolvePaths(Annotation annotation) {
        return switch (annotation) {
            case RequestMapping a -> resolveValueOrPath(a.value(), a.path());
            case GetMapping a -> resolveValueOrPath(a.value(), a.path());
            case PostMapping a -> resolveValueOrPath(a.value(), a.path());
            case PutMapping a -> resolveValueOrPath(a.value(), a.path());
            case DeleteMapping a -> resolveValueOrPath(a.value(), a.path());
            default -> new String[0];
        };
    }

    private String[] resolveValueOrPath(String[] value, String[] path) {
        if (value.length > 0) {
            return value;
        }
        if (path.length > 0) {
            return path;
        }
        return new String[0];
    }
}
