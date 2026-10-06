package com.carlos.org.exception;


import com.carlos.core.exception.GlobalException;
import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 组件异常父类
 * </p>
 *
 * @author carlos
 * @date 2022/10/24 13:19
 */
public class OrgModuleException extends GlobalException {

    private static final long serialVersionUID = -2303357122330162359L;

    public OrgModuleException() {
        super("组件异常！");
    }

    public OrgModuleException(String message) {
        super(message);
    }

    public OrgModuleException(Throwable cause) {
        super(cause);
    }

    public OrgModuleException(ErrorCode errorCode) {
        super(errorCode);
    }

    public OrgModuleException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public OrgModuleException(String errorCode, String message) {
        super(errorCode, message);
    }

    public OrgModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
