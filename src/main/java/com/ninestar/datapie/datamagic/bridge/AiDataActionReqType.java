package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONObject;

//camel style to match with column name of DB
public class AiDataActionReqType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public Integer modelId;
    public JSONObject fieldMap;
    public Boolean pubFlag;
}

