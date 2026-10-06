package com.carlos.license;

import com.carlos.license.service.AbstractSystemInfoDao;
import com.carlos.license.service.LinuxSystemInfoDao;
import com.carlos.license.service.WindowsSystemInfoDao;
import de.schlichtherle.license.*;
import de.schlichtherle.xml.GenericCertificate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.beans.ExceptionListener;
import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Carlos
 * 自定义 License 管理，创建、安装、校验等
 */
@Slf4j
public class CustomLicenseManager extends LicenseManager {
    /**
     * XML 编码
     */
    private static final String XML_CHARSET = "UTF-8";
    /**
     * 默认 BUFSIZE
     */
    private static final int DEFAULT_BUFSIZE = 8 * 1024;

    /**
     * XMLDecoder 反序列化允许的白名单类
     */
    private static final Set<String> ALLOWED_CLASSES = Set.of(
        "java.beans.XMLDecoder",
        "java",
        "de.schlichtherle.license.LicenseContent",
        "com.carlos.license.LicenseCheckModel",
        "java.util.Date",
        "java.util.ArrayList",
        "javax.security.auth.x500.X500Principal",
        "java.lang.String",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Boolean",
        "java.lang.Double",
        "java.lang.Float",
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Character"
    );

    /**
     * XMLDecoder 允许的方法调用白名单
     */
    private static final Set<String> ALLOWED_METHODS = Set.of("add", "get", "set", "put", "toArray");

    private final ReentrantLock lock = new ReentrantLock();

    public CustomLicenseManager(LicenseParam param) {
        super(param);
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: CustomLicenseManager.java </p>
     * <p>方法描述: 重写 License 创建 </p>
     * <p>创建时间: 2025/04/10 13:11 </p>
     *
     * @param content LicenseContent
     * @param notary  LicenseNotary
     * @return byte[]
     * @author Carlos
     * @version 1.0
     */
    @Override
    protected byte[] create(LicenseContent content, LicenseNotary notary) throws Exception {
        lock.lock();
        try {
            initialize(content);
            this.validateCreate(content);
            final GenericCertificate certificate = notary.sign(content);
            return getPrivacyGuard().cert2key(certificate);
        } finally {
            lock.unlock();
        }
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: CustomLicenseManager.java </p>
     * <p>方法描述: 重写 License 安装 </p>
     * <p>创建时间: 2025/04/10 13:13 </p>
     *
     * @param key    key
     * @param notary LicenseNotary
     * @return de.schlichtherle.license.LicenseContent
     * @author Carlos
     * @version 1.0
     */
    @Override
    protected LicenseContent install(final byte[] key, final LicenseNotary notary) throws Exception {
        lock.lock();
        try {
            final GenericCertificate certificate = getPrivacyGuard().key2cert(key);

            notary.verify(certificate);
            final LicenseContent content = (LicenseContent) this.load(certificate.getEncoded());
            this.validate(content);
            setLicenseKey(key);
            setCertificate(certificate);

            return content;
        } finally {
            lock.unlock();
        }
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: CustomLicenseManager.java </p>
     * <p>方法描述: 重写 License 校验 </p>
     * <p>创建时间: 2025/04/10 13:14 </p>
     *
     * @param notary LicenseNotary
     * @return de.schlichtherle.license.LicenseContent
     * @author Carlos
     * @version 1.0
     */
    @Override
    protected LicenseContent verify(final LicenseNotary notary) throws Exception {
        lock.lock();
        try {
            GenericCertificate certificate;

            // Load license key from preferences,
            final byte[] key = getLicenseKey();
            if (null == key) {
                throw new NoLicenseInstalledException(getLicenseParam().getSubject());
            }

            certificate = getPrivacyGuard().key2cert(key);
            notary.verify(certificate);
            final LicenseContent content = (LicenseContent) this.load(certificate.getEncoded());
            this.validate(content);
            setCertificate(certificate);

            return content;
        } finally {
            lock.unlock();
        }
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: CustomLicenseManager.java </p>
     * <p>方法描述: 校验生成证书的参数信息 </p>
     * <p>创建时间: 2025/04/10 13:14 </p>
     *
     * @param content LicenseContent
     * @return void
     * @author Carlos
     * @version 1.0
     */
    protected void validateCreate(final LicenseContent content) throws LicenseContentException {
        lock.lock();
        try {
            final Date now = new Date();
            final Date notBefore = content.getNotBefore();
            final Date notAfter = content.getNotAfter();
            if (null != notAfter && now.after(notAfter)) {
                throw new LicenseContentException("证书失效时间不能早于当前时间");
            }
            if (null != notBefore && null != notAfter && notAfter.before(notBefore)) {
                throw new LicenseContentException("证书生效时间不能晚于证书失效时间");
            }
            final String consumerType = content.getConsumerType();
            if (null == consumerType) {
                throw new LicenseContentException("用户类型不能为空");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: CustomLicenseManager.java </p>
     * <p>方法描述: 重写 License 验证 </p>
     * <p>创建时间: 2025/04/10 13:15 </p>
     *
     * @param content LicenseContent
     * @return void
     * @author Carlos
     * @version 1.0
     */
    @Override
    protected void validate(final LicenseContent content) throws LicenseContentException {
        lock.lock();
        try {
            // 1. 首先调用父类的validate方法
            super.validate(content);
            // 2. 然后校验自定义的License参数
            // License中可被允许的参数信息
            LicenseCheckModel expectedCheckModel = (LicenseCheckModel) content.getExtra();
            // 当前服务器真实的参数信息
            LicenseCheckModel serverCheckModel = getServerInfos();

            if (expectedCheckModel != null && serverCheckModel != null) {
                // 校验IP地址
                if (!checkIpAddress(expectedCheckModel.getIpAddress(), serverCheckModel.getIpAddress())) {
                    throw new LicenseContentException("当前服务器的IP没在授权范围内");
                }

                // 校验Mac地址
                if (!checkIpAddress(expectedCheckModel.getMacAddress(), serverCheckModel.getMacAddress())) {
                    throw new LicenseContentException("当前服务器的Mac地址没在授权范围内");
                }

                // 校验主板序列号
                if (!checkSerial(expectedCheckModel.getMainBoardSerial(), serverCheckModel.getMainBoardSerial())) {
                    throw new LicenseContentException("当前服务器的主板序列号没在授权范围内");
                }

                // 校验CPU序列号
                if (!checkSerial(expectedCheckModel.getCpuSerial(), serverCheckModel.getCpuSerial())) {
                    throw new LicenseContentException("当前服务器的CPU序列号没在授权范围内");
                }
            } else {
                throw new LicenseContentException("不能获取服务器硬件信息");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: CustomLicenseManager.java </p>
     * <p>方法描述: XMLDecoder 解析 XML（已增加安全白名单校验） </p>
     * <p>创建时间: 2025/04/10 13:16 </p>
     *
     * @param encoded encoded
     * @return java.lang.Object
     * @author Carlos
     * @version 1.0
     */
    private Object load(String encoded) {
        if (!StringUtils.hasText(encoded)) {
            return null;
        }

        try {
            validateXmlContent(encoded);
        } catch (SecurityException e) {
            log.error("License XML 内容安全校验失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("License XML 内容校验异常", e);
            throw new SecurityException("License XML 内容未通过安全校验", e);
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(encoded.getBytes(XML_CHARSET));
             BufferedInputStream bis = new BufferedInputStream(bais, DEFAULT_BUFSIZE);
             XMLDecoder decoder = new XMLDecoder(bis, null, new XmlDecodeExceptionListener())) {
            return decoder.readObject();
        } catch (Throwable e) {
            log.error("XML 解析失败：不支持的编码", e);
        }
        return null;
    }

    /**
     * 校验 XML 内容，确保不包含危险的类和标签
     *
     * @param xml XML 字符串
     */
    private void validateXmlContent(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // 禁用 DTD，防止 XXE 攻击
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes(XML_CHARSET))) {
            org.w3c.dom.Document doc = builder.parse(bais);

            // 校验 <object> 标签的 class 属性
            NodeList objectNodes = doc.getElementsByTagName("object");
            for (int i = 0; i < objectNodes.getLength(); i++) {
                Element element = (Element) objectNodes.item(i);
                String className = element.getAttribute("class");
                if (StringUtils.hasText(className) && !isAllowedClass(className)) {
                    throw new SecurityException("不允许的反序列化类: " + className);
                }
            }

            // 校验 <array> 标签的 class 属性
            NodeList arrayNodes = doc.getElementsByTagName("array");
            for (int i = 0; i < arrayNodes.getLength(); i++) {
                Element element = (Element) arrayNodes.item(i);
                String className = element.getAttribute("class");
                if (StringUtils.hasText(className) && !isAllowedClass(className)) {
                    throw new SecurityException("不允许的反序列化数组类: " + className);
                }
            }

            // 校验 <void> 标签的 method 属性
            NodeList voidNodes = doc.getElementsByTagName("void");
            for (int i = 0; i < voidNodes.getLength(); i++) {
                Element element = (Element) voidNodes.item(i);
                String methodName = element.getAttribute("method");
                if (StringUtils.hasText(methodName) && !ALLOWED_METHODS.contains(methodName)) {
                    throw new SecurityException("不允许的方法调用: " + methodName);
                }
            }
        }
    }

    /**
     * 判断类名是否在白名单中
     *
     * @param className 类名
     * @return 是否允许
     */
    private boolean isAllowedClass(String className) {
        // 允许基础类型数组
        if (className.startsWith("[") && className.length() == 2) {
            return true;
        }
        // 允许对象类型数组，递归检查元素类型
        if (className.startsWith("[L") && className.endsWith(";")) {
            String elementClass = className.substring(2, className.length() - 1);
            return isAllowedClass(elementClass);
        }
        // 允许多维数组
        if (className.startsWith("[")) {
            return isAllowedClass(className.substring(1));
        }
        // 白名单匹配
        return ALLOWED_CLASSES.contains(className);
    }

    /**
     * XMLDecoder 异常监听器，防止反序列化异常信息泄露
     */
    private static class XmlDecodeExceptionListener implements ExceptionListener {
        @Override
        public void exceptionThrown(Exception e) {
            log.error("XMLDecoder 反序列化过程中发生异常", e);
        }
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: CustomLicenseManager.java </p>
     * <p>方法描述: 获取当前服务器需要额外校验的 License 参数 </p>
     * <p>创建时间: 2025/04/10 13:16 </p>
     *
     * @return com.carlos.license.LicenseCheckModel
     * @author Carlos
     * @version 1.0
     */
    private LicenseCheckModel getServerInfos() {
        //操作系统类型
        String osName = System.getProperty("os.name").toLowerCase();
        AbstractSystemInfoDao abstractSystemInfoDao;

        //根据不同操作系统类型选择不同的数据获取方法
        if (osName.startsWith("windows")) {
            abstractSystemInfoDao = new WindowsSystemInfoDao();
        } else if (osName.startsWith("linux")) {
            abstractSystemInfoDao = new LinuxSystemInfoDao();
        } else {//其他服务器类型
            abstractSystemInfoDao = new LinuxSystemInfoDao();
        }

        return abstractSystemInfoDao.getServerInfos();
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: CustomLicenseManager.java </p>
     * <p>方法描述: 校验当前服务器的IP/Mac地址是否在可被允许的IP范围内 </p>
     * <p>创建时间: 2025/04/10 13:17 </p>
     *
     * @param expectedList expectedList
     * @param serverList   serverList
     * @return boolean
     * @author Carlos
     * @version 1.0
     */
    private boolean checkIpAddress(List<String> expectedList, List<String> serverList) {
        if (expectedList != null && expectedList.size() > 0) {
            if (serverList != null && serverList.size() > 0) {
                for (String expected : expectedList) {
                    if (serverList.contains(expected.trim())) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return true;
        }
    }

    /**
     * <p>项目名称: true-license-demo </p>
     * <p>文件名称: CustomLicenseManager.java </p>
     * <p>方法描述: 校验当前服务器硬件（主板、CPU 等）序列号是否在可允许范围内 </p>
     * <p>创建时间: 2025/04/10 13:18 </p>
     *
     * @param expectedSerial expectedSerial
     * @param serverSerial   serverSerial
     * @return boolean
     * @author Carlos
     * @version 1.0
     */
    private boolean checkSerial(String expectedSerial, String serverSerial) {
        if (StringUtils.hasText(expectedSerial)) {
            if (StringUtils.hasText(serverSerial)) {
                return expectedSerial.equals(serverSerial);
            }
            return false;
        } else {
            return true;
        }
    }
}
