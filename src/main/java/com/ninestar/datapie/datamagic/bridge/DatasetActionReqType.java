package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class DatasetActionReqType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public JSONArray variable;
    public String content;
    public JSONArray fields;
    public JSONObject graph;
    public String graphVer;
    public Boolean pubFlag;
    public Integer sourceId;
}

