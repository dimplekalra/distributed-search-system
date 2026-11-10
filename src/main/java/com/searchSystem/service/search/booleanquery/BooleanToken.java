package com.searchSystem.service.search.booleanquery;

public class BooleanToken {
    private String value;

    private BooleanOperator operator;

    public BooleanToken(
            String value,
            BooleanOperator operator) {

        this.value = value;
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public BooleanOperator getOperator() {
        return operator;
    }
}
