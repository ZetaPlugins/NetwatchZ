package com.zetaplugins.netwatchz.common.ipapi;

public class IpDataFetchException extends RuntimeException {
    public IpDataFetchException(String message) {
        super(message);
    }

    public IpDataFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
