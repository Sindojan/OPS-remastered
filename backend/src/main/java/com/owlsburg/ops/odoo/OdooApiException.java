package com.owlsburg.ops.odoo;

public class OdooApiException extends RuntimeException {

    public OdooApiException(String message) {
        super(message);
    }

    public OdooApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
