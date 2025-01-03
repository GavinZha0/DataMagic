package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class MlDatasetListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String type;
    public JSONArray variable;
    public String content;
    public JSONArray fields;
    public JSONArray transform;
    //public JSONArray target;
    public List<String> target = new ArrayList<>();
    public Integer fCount;
    public Integer volume;
    public Boolean pubFlag;
    public Integer usage;
    public Integer sourceId;
    public String sourceName;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

