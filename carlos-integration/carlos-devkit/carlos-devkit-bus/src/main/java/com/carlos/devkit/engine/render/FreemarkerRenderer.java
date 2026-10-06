package com.carlos.devkit.engine.render;

import com.carlos.devkit.config.DevkitProperties;
import com.carlos.devkit.enums.DevkitErrorCode;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * FreeMarker 渲染器
 * </p>
 * <p>
 * 与 carlos-tools 原 {@code TemplateUtil} 的关键差异：
 * <ul>
 *   <li><b>每次渲染新建 Configuration</b>：原实现用静态 Configuration +
 *       {@code setDirectoryForTemplateLoading}，多用户并发下会互相踩踏；</li>
 *   <li><b>安全策略</b>：模板可由用户在线编辑，必须禁掉 {@code ?new} 与 {@code ?api}
 *       （否则等同于服务端任意代码执行）；</li>
 *   <li><b>输出限额 + 渲染超时</b>：防超大输出与死循环模板；</li>
 *   <li><b>显式 UTF-8</b>：原实现用平台默认编码写文件（Windows 下是 GBK）。</li>
 * </ul>
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
public class FreemarkerRenderer {

    private final DevkitProperties devkitProperties;

    private final ExecutorService renderExecutor;

    public FreemarkerRenderer(final DevkitProperties devkitProperties,
                              @Qualifier("devkitRenderExecutor") final ExecutorService renderExecutor) {
        this.devkitProperties = devkitProperties;
        this.renderExecutor = renderExecutor;
    }

    /**
     * 渲染模板到目标文件
     *
     * @param templateFile 模板文件（.ftl）
     * @param params       渲染参数
     * @param targetFile   目标文件
     */
    public void renderToFile(final File templateFile, final Map<String, Object> params, final File targetFile) {
        String content = render(templateFile, params);
        try {
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) {
                Files.createDirectories(parent.toPath());
            }
            Files.writeString(targetFile.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("产物写入失败: {}", targetFile.getPath(), e);
            throw DevkitErrorCode.GENERATE_FAILED.exception("产物写入失败：" + targetFile.getName());
        }
        log.debug("模板渲染成功: {} -> {}", templateFile.getName(), targetFile.getName());
    }

    /**
     * 渲染模板为字符串
     *
     * @param templateFile 模板文件（.ftl）
     * @param params       渲染参数
     * @return 渲染结果
     */
    public String render(final File templateFile, final Map<String, Object> params) {
        Future<String> future = renderExecutor.submit(() -> doRender(templateFile, params));
        try {
            return future.get(devkitProperties.getRenderTimeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("模板渲染超时({}ms): {}", devkitProperties.getRenderTimeoutMs(), templateFile.getName());
            throw DevkitErrorCode.TEMPLATE_RENDER_TIMEOUT.exception(templateFile.getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw DevkitErrorCode.TEMPLATE_RENDER_FAILED.exception("渲染被中断：" + templateFile.getName());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            log.error("模板渲染失败: {}", templateFile.getPath(), cause);
            throw DevkitErrorCode.TEMPLATE_RENDER_FAILED.exception(
                templateFile.getName() + "：" + cause.getMessage());
        }
    }

    /**
     * 实际渲染（在渲染线程池中执行）
     *
     * @param templateFile 模板文件
     * @param params       渲染参数
     * @return 渲染结果
     * @throws Exception 渲染异常
     */
    private String doRender(final File templateFile, final Map<String, Object> params) throws Exception {
        Configuration configuration = buildSecureConfiguration(templateFile.getParentFile());
        Template template = configuration.getTemplate(templateFile.getName(), StandardCharsets.UTF_8.name());
        try (Writer writer = new LimitedStringWriter(devkitProperties.getMaxSingleFileSize())) {
            template.process(params, writer);
            return writer.toString();
        }
    }

    /**
     * 构建带安全策略的 FreeMarker 配置（每次渲染独立实例，天然线程安全）
     *
     * @param templateDir 模板所在目录
     * @return FreeMarker 配置
     * @throws IOException 目录不可读时抛出
     */
    private Configuration buildSecureConfiguration(final File templateDir) throws IOException {
        Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setDirectoryForTemplateLoading(templateDir);
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setOutputEncoding(StandardCharsets.UTF_8.name());
        configuration.setLocalizedLookup(false);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setLogTemplateExceptions(false);
        configuration.setWrapUncheckedExceptions(true);
        // 安全三件套：禁 ?new（任意类实例化）、禁 ?api（反射）、禁用静态方法访问
        configuration.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
        configuration.setAPIBuiltinEnabled(false);
        return configuration;
    }

    /**
     * <p>
     * 带大小上限的字符串写入器
     * </p>
     * <p>
     * 模板可由用户在线编辑，必须防止单个模板产出超大内容（或递归模板打爆内存）。
     * </p>
     */
    private static final class LimitedStringWriter extends StringWriter {

        private final long maxSize;

        private LimitedStringWriter(final long maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public void write(final int c) {
            check(1);
            super.write(c);
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) {
            check(len);
            super.write(cbuf, off, len);
        }

        @Override
        public void write(final String str) {
            check(str.length());
            super.write(str);
        }

        @Override
        public void write(final String str, final int off, final int len) {
            check(len);
            super.write(str, off, len);
        }

        private void check(final int append) {
            if (getBuffer().length() + (long) append > maxSize) {
                throw DevkitErrorCode.GENERATE_OUTPUT_TOO_LARGE.exception(
                    "单个文件产物超过 " + maxSize + " 字节上限");
            }
        }
    }
}
