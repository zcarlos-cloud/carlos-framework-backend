package com.carlos.oss.web;

import com.carlos.oss.core.OssTemplate;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.InputStream;

/**
 * <p>
 * OSS 对象返回值处理器
 * 如果接口返回的是 OssObject，则自动处理为文件下载
 * </p>
 *
 * @author carlos
 * @date 2026-03-06
 */
public class OssObjectHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final OssTemplate ossTemplate;

    public OssObjectHandlerMethodReturnValueHandler(OssTemplate ossTemplate) {
        this.ossTemplate = ossTemplate;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return returnType.getParameterType() == OssObject.class;
    }

    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType,
                                  ModelAndViewContainer mavContainer, NativeWebRequest webRequest) {
        final OssObject ossObject = (OssObject) returnValue;
        final String bucket = ossObject.getBucket();
        final String object = ossObject.getObject();
        final InputStream inputStream = ossTemplate.getObject(bucket, object);

        mavContainer.setView(new OssObjectView(inputStream, ossObject.getAttachmentName()));
    }
}
