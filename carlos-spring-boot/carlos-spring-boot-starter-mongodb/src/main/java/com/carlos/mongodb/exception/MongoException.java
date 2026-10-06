package com.carlos.mongodb.exception;


import com.carlos.core.exception.ComponentException;

/**
 * <p>
 * MongoDB 异常父类
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 */
public class MongoException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public MongoException() {
        super("MongoDB operation exception!");
    }

    public MongoException(String message) {
        super(message);
    }

    public MongoException(Throwable cause) {
        super(cause);
    }

    public MongoException(String message, Throwable cause) {
        super(message, cause);
    }
}
