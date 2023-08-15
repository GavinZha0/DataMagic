package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class MlModelListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public JSONObject config;
    public String content;
    public String type;
    public Boolean pubFlag;
    public String framework;
    public String frameVer;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

