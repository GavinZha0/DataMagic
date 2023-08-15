package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class AiImageActionReqType {
    public Integer id;
    public String name;
    public String description;
    public String category;
    public String type;
    public String field;
    public Integer modelId;
    public String platform;
    public String platformVer;
    public JSONArray content;
    public Boolean pubFlag;
}

