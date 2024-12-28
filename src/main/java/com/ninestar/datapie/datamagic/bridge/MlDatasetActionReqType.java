package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class MlDatasetActionReqType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public JSONArray variable;
    public String type;
    public String content;
    public JSONArray fields;
    public JSONArray transform;
    public List<String> target = new ArrayList<>();
    public Boolean pubFlag;
    public Integer sourceId;
}

