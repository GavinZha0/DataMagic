package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

//camel style to match with column name of DB
public class MlFeatureActionReqType {
    public Integer id;
    public String name;
    public String type;
    public String desc;
    public String group;
    public Integer sourceId;
    public String sourceName;
    public String datasetName;
    public String query;
    public JSONArray fields;
    public String target;
    public Integer features;
    public JSONArray relPair;
    public Boolean pubFlag;
}

