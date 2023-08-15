package com.ninestar.datapie.datamagic.consts;

public enum BoolOperator {
    /**
     * and
     */
    AND("and"),

    /**
     * or
     */
    OR("or");

    private final String code;


    BoolOperator(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
