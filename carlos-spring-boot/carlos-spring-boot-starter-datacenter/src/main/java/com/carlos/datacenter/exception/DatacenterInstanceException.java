package com.carlos.datacenter.exception;

import com.carlos.core.exception.BusinessException;

public class DatacenterInstanceException extends BusinessException {
    private static final long serialVersionUID = 3196735788466764225L;
    private String requestId;

    public DatacenterInstanceException(String message) {
        super(message);
    }

    public DatacenterInstanceException(String code, String message) {
        super(code, message);
    }

    public DatacenterInstanceException(String code, String message, String requestId) {
        super(code, message);
        this.requestId = requestId;
    }

    public DatacenterInstanceException(Exception e) {
        super(e);
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
