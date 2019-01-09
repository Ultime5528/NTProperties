package com.ultime5528.ntproperties;

public class NTPropertiesException extends RuntimeException {

    private static final long serialVersionUID = -3765711967194697018L;

    public NTPropertiesException(String message) {
        super(message);
    }

    public NTPropertiesException(String message, Throwable ex) {
        super(message, ex);
    }

}