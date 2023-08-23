package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

//camel style to match with column name of DB
public class DatareportActionReqType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String type;
    public JSONArray viewIds;
    public JSONArray pages;
    public Boolean pubFlag;
}

