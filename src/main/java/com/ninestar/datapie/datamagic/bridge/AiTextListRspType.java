package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class AiTextListRspType {
    public Integer id;
    public String name;
    public String description;
    public String group;
    public String type;
    public String area;
    public Integer modelId;
    public String modelName;
    public String platform;
    public String platformVer;
    public JSONArray content;
    public Boolean pubFlag;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

