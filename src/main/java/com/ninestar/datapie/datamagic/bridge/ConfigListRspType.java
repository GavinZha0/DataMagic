package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class ConfigListRspType {
    public Integer id;
    public String name;
    public String module;
    public String group;
    public String type;
    public String value;
    public String previous;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

