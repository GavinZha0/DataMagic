package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONObject;

//camel style to match with column name of DB
public class MlModelActionReqType {
    public Integer id;
    public Integer pid;
    public String name;
    public String type;
    public String description;
    public String category;
    public String framework;
    public String frameVer;
    public JSONObject config;
    public String content;
    public Boolean pubFlag;
}

