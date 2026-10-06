package com.carlos.datacenter.exception;

import com.carlos.core.exception.BusinessException;

public class DatacenterSqlException extends BusinessException {
    private static final long serialVersionUID = 5072304707119333642L;

    public DatacenterSqlException(String message) {
        super(message);
    }

}
