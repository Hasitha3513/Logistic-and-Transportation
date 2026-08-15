package com.transportlogistics.app.shared.domain;

/** A client-correctable business validation failure with a stable API error code. */
public class BusinessRuleException extends IllegalArgumentException {
    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
