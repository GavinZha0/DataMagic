package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class AiDataListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String dataset;
    public String area;
    public JSONObject fieldMap;
    public Integer modelId;
    public String modelName;
    public Integer status;
    public Boolean pubFlag;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

