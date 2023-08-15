package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class AiImageListRspType {
    public Integer id;
    public String name;
    public String description;
    public String category;
    public String type;
    public String field;
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

