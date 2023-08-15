package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

//camel style to match with column name of DB
public class AlgorithmActionReqType {
    public Integer id;
    public Integer pid;
    public String name;
    public String type;
    public String desc;
    public String group;
    public String language;
    public String langVer;
    public JSONObject config;
    public String content;
    public Boolean pubFlag;
}

