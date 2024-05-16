package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class WorkflowActionReqType {
    public Integer id;
    public Integer pid;
    public String name;
    public String desc;
    public String group;
    public JSONObject workflow;
    public String x6Ver;
    public String version;
    public JSONObject canvas;
    public JSONObject config;
    public Timestamp lastRun;
    public Integer duration;
    public String status;
    public String error;
    public Boolean pubFlag;
}

