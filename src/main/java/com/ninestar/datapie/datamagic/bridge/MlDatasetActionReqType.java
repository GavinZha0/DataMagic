package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

//camel style to match with column name of DB
public class MlDatasetActionReqType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public JSONArray variable;
    public String query;
    public JSONArray fields;
    public JSONArray target;
    public Boolean pubFlag;
    public Integer sourceId;
}

