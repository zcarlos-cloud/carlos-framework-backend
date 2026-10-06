package com.carlos.boot.web.enums;

import cn.hutool.core.collection.CollectionUtil;
import com.carlos.boot.web.config.EnumDictProperties;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举字典服务，使用 Spring 内置 {@link ClassPathScanningCandidateComponentProvider} 扫描实现
 * {@link BaseEnum} 的枚举类并缓存以供查询。
 *
 * <p>相比旧版使用 org.reflections 的实现，本版本：</p>
 * <ul>
 *   <li>完全兼容 Spring 生命周期</li>
 *   <li>GraalVM Native Image 兼容</li>
 *   <li>无第三方扫描库依赖</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class EnumDictService {

    private final EnumDictProperties properties;

    /** 枚举字典缓存（不可变） */
    private final Map<String, List<EnumInfo>> dictCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            Set<String> packages = properties.getScanPackages();
            if (CollectionUtil.isEmpty(packages)) {
                log.debug("EnumDictService: no scan packages configured, skipping");
                return;
            }

            ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AssignableTypeFilter(BaseEnum.class));

            int totalCount = 0;
            for (String pkg : packages) {
                for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends BaseEnum<?>> enumClass =
                            (Class<? extends BaseEnum<?>>) Class.forName(bd.getBeanClassName());
                        processEnumClass(enumClass);
                        totalCount++;
                    } catch (Exception e) {
                        log.warn("EnumDictService: failed to process class {}", bd.getBeanClassName(), e);
                    }
                }
            }

            log.info("EnumDictService: scanned {} enum classes across packages {}", totalCount, packages);
        } catch (Exception e) {
            log.error("EnumDictService: initialization failed", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processEnumClass(Class<? extends BaseEnum<?>> enumClass) {
        AppEnum annotation = enumClass.getAnnotation(AppEnum.class);
        if (annotation == null) {
            log.warn("EnumDictService: class {} missing @AppEnum annotation, skipping", enumClass.getSimpleName());
            return;
        }

        BaseEnum<?>[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return;
        }

        List<EnumInfo> list = new ArrayList<>();
        for (BaseEnum<?> baseEnum : constants) {
            EnumInfo info = new EnumInfo();
            info.setCode(baseEnum.getCode());
            info.setDesc(baseEnum.getDesc());
            info.setName(baseEnum.toString());
            info.setDescEn(baseEnum.getDescEn());
            info.setColor(baseEnum.getColor());
            list.add(info);
        }

        dictCache.put(annotation.code(), list);
        log.debug("EnumDictService: loaded {} entries for '{}'", list.size(), annotation.code());
    }

    /**
     * 获取全部枚举字典（不可变副本）。
     */
    public Map<String, List<EnumInfo>> getDict() {
        return Collections.unmodifiableMap(dictCache);
    }

    /**
     * 获取指定枚举类的详情。
     *
     * @param code 枚举标识（{@link AppEnum#code()}）
     */
    public List<EnumInfo> getDictEntry(String code) {
        List<EnumInfo> list = dictCache.get(code);
        if (list == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(list);
    }
}
