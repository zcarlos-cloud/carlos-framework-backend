package com.carlos.devkit.engine.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import com.carlos.devkit.engine.DevkitConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 数据库数据类型 → Java 类型映射
 * </p>
 * <p>
 * 迁移自 carlos-tools 原 {@code XmlUtils.readConvertXml}，改为从 classpath 读取
 * {@code devkit/typeConverter.xml}（不再依赖临时目录解包）。
 * 该映射是引擎配置而非用户可编辑的模板资产，故不入库。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
public final class TypeConverter {

    /**
     * 数据库类型 → Java 全限定类型
     */
    private static final Map<String, String> CONVERT_MAP = load();

    /**
     * 映射缺失时的兜底 Java 类型
     */
    private static final String DEFAULT_JAVA_TYPE = "java.lang.String";

    private TypeConverter() {
    }

    /**
     * 读取映射配置（类加载时执行一次）
     *
     * @return 只读映射表
     */
    private static Map<String, String> load() {
        Map<String, String> map = new HashMap<>(64);
        ClassPathResource resource = new ClassPathResource(DevkitConstants.TYPE_CONVERTER_RESOURCE);
        try (InputStream inputStream = resource.getInputStream()) {
            Document document = XmlUtil.readXML(inputStream);
            Element rootElement = XmlUtil.getRootElement(document);
            List<Element> typeElements = XmlUtil.getElements(rootElement, "type");
            for (Element element : typeElements) {
                map.put(element.getAttribute("key"), element.getAttribute("value"));
            }
        } catch (Exception e) {
            log.error("数据库类型映射配置读取失败: {}", DevkitConstants.TYPE_CONVERTER_RESOURCE, e);
            throw new IllegalStateException("数据库类型映射配置读取失败：" + DevkitConstants.TYPE_CONVERTER_RESOURCE, e);
        }
        log.info("数据库类型映射加载完成，共 {} 条", map.size());
        return Collections.unmodifiableMap(map);
    }

    /**
     * 取数据库类型对应的 Java 全限定类型
     *
     * @param columnDbType 数据库数据类型（如 VARCHAR）
     * @return Java 全限定类型，未命中返回 {@code java.lang.String}
     */
    public static String getFullJavaType(final String columnDbType) {
        String javaType = CONVERT_MAP.get(columnDbType);
        return StrUtil.isBlank(javaType) ? DEFAULT_JAVA_TYPE : javaType;
    }
}
