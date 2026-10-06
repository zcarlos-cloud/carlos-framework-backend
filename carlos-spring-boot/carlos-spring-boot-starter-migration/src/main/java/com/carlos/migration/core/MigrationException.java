package com.carlos.migration.core;

import com.carlos.core.exception.GlobalException;

/**
 * 数据库迁移异常
 *
 * @author carlos
 * @since 3.0.0
 */
public class MigrationException extends GlobalException {

    public MigrationException(String message) {
        super(message);
    }

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MigrationException(Throwable cause) {
        super(cause);
    }
}
