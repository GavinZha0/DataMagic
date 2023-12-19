package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class MlFeatureListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String type;
    public Integer sourceId;
    public String sourceName;
    public String datasetName;
    public String query;
    public JSONArray fields;
    public Integer features;
    public String target;
    public JSONArray relPair;
    public Boolean pubFlag;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

