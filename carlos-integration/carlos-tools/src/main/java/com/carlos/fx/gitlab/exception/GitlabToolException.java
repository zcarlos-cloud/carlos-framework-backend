package com.carlos.fx.gitlab.exception;

/**
 * <p>
 * 代码生成器异常
 * </p>
 *
 * @author Carlos
 * @date 2021/9/27 10:44
 */
public class GitlabToolException extends RuntimeException {

    public GitlabToolException() {
        super("Gitlab Tool Exception！");
    }

    public GitlabToolException(String message) {
        super(message);
    }

    public GitlabToolException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitlabToolException(Throwable cause) {
        super(cause);
    }

    protected GitlabToolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
