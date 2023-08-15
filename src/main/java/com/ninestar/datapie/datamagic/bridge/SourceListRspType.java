package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class SourceListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String type;
    public String url;
    public String username;
    public String password;
    public String version;
    public JSONArray params;
    public Boolean pubFlag;
    public Integer usage;

    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}


