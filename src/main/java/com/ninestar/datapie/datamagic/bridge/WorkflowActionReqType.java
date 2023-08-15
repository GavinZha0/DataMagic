package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class WorkflowActionReqType {
    public Integer id;
    public Integer pid;
    public String name;
    public String description;
    public String category;
    public JSONObject graph;
    public String graphVer;
    public String version;
    public JSONObject grid;
    public JSONArray config;
    public Timestamp lastRun;
    public Integer duration;
    public String status;
    public String error;
    public Boolean pubFlag;
}

