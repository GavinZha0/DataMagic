package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;

import java.sql.Timestamp;
import java.util.List;

//camel style to match with column name of DB
public class SourceActionReqType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String type;
    public String url;
    public JSONArray params;
    public String username;
    public String password;
    public String version;
    public Boolean pubFlag;
}

