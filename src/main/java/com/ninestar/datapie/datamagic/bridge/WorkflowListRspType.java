package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class WorkflowListRspType {
    public Integer id;
    public Integer pid;
    public String name;
    public String desc;
    public String group;
    public String flowVer;
    public JSONObject workflow;
    public JSONObject canvas;
    public JSONObject config;
    public Timestamp lastRun;
    // unit min
    public Integer duration;
    public String status;
    public String error;
    public Boolean pubFlag;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

