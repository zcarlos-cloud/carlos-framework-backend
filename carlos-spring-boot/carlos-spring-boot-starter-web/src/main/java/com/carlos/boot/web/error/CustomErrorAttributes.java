package com.carlos.boot.web.error;

import com.carlos.core.exception.GlobalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * <p>
 * 自定义错误属性扩展。
 * </p>
 * <p>
 * 继承 {@link DefaultErrorAttributes}，当异常为 {@link GlobalException} 及其子类时，
 * 在错误属性 Map 中追加 {@code code} 字段，便于前端获取业务错误码。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
public class CustomErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
        Throwable error = getError(webRequest);
        if (error instanceof GlobalException globalEx) {
            errorAttributes.put("code", globalEx.getErrorCode());
        }
        return errorAttributes;
    }

}
