package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class MlDatasetListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public JSONArray variable;
    public String query;
    public JSONArray fields;
    public JSONArray target;
    public Integer fCount;
    public Boolean pubFlag;
    public Integer usage;
    public Integer sourceId;
    public String sourceName;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

