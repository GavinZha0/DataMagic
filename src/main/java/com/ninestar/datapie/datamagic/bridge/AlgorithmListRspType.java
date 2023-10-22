package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class AlgorithmListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public JSONArray config;
    public String content;
    public String type;
    public Boolean pubFlag;
    public String language;
    public String langVer;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

