package com.ninestar.datapie.datamagic.consts;

public enum DatasourceType {

    JDBC("jdbc"),
    CSV("csv");

    private String type;

    DatasourceType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
