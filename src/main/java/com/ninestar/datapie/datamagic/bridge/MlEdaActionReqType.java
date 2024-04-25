package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONObject;

//camel style to match with column name of DB
public class MlEdaActionReqType {
    public Integer id;
    public String name;
    public String type;
    public String desc;
    public String group;
    public Integer datasetId;
    public String datasetName;
    public JSONObject config;
    public Boolean pubFlag;
}

