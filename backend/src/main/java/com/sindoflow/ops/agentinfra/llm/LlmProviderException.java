package com.sindoflow.ops.agentinfra.llm;

public class LlmProviderException extends RuntimeException {

    private final Integer statusCode;

    public LlmProviderException(String message) {
        super(message);
        this.statusCode = null;
    }

    public LlmProviderException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public LlmProviderException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public LlmProviderException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
