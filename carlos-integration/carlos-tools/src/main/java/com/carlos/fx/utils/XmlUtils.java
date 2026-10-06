package com.carlos.fx.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.util.XmlUtil;
import com.carlos.fx.codege.config.CodegeConstant;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 处理xml文件的相关工具类
 * </p>
 *
 * @author Carlos
 * @date 2021/10/26 12:07
 */
@Slf4j
public class XmlUtils {

    public static final String TYPE_CONVERT_XML = "typeConverter.xml";

    /**
     * 解析对象转换xml文件
     *
     * @return Map xml文件对应的Map
     * @author Carlos
     * @date 2019/6/10 13:35
     */
    public static Map<String, String> readConvertXml() {

        File file = new File(CodegeConstant.TEMP_DIR + File.separator + TYPE_CONVERT_XML);
        if (!file.exists()) {
            log.error("can't find file:{}", file.getPath());
            throw new IllegalStateException("类型配置文件不存在！");
        }
        Document document;
        try {
            document = XmlUtil.readXML(file);
        } catch (UtilException e) {
            log.error("convert文件读取失败", e);
            throw new IllegalStateException();
        }
        Element rootElement = XmlUtil.getRootElement(document);
        List<Element> typeElements = XmlUtil.getElements(rootElement, "type");
        Map<String, String> map = new HashMap<>(16);
        for (Element element : typeElements) {
            map.put(element.getAttribute("key"), element.getAttribute("value"));
        }
        return map;
    }

    /**
     * 读取模板描述信息
     *
     * @param templateFile 模板配置文件
     * @return Map xml文件对应的Map
     * @author Carlos
     * @date 2019/6/10 13:35
     */
    public static <T> T readTemplateBaseInfo(File templateFile, Class<T> clazz) {
        Document document;
        try {
            document = XmlUtil.readXML(templateFile);
        } catch (Exception e) {
            log.error("模板描述文件读取失败， path={}", templateFile.getPath(), e);
            throw new IllegalStateException();
        }
        Element rootElement = XmlUtil.getRootElement(document);
        Map<String, Object> stringObjectMap = XmlUtil.xmlToMap(rootElement);
        T templateInfo = BeanUtil.toBean(stringObjectMap, clazz, CopyOptions.create().setAutoTransCamelCase(true));
        return templateInfo;
    }
}
