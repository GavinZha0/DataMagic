package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class DataviewListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String type;
    public List<String> dim = new ArrayList<>();
    public List<String> relation = new ArrayList<>();
    public List<String> location = new ArrayList<>();
    public List<String> metrics = new ArrayList<>();
    public String agg;
    public Integer prec;
    public JSONObject filter;
    public JSONArray sorter;
    public JSONArray variable;
    public JSONArray calculation;
    public String modelType;
    public JSONObject model;
    public String libName;
    public String libVer;
    public JSONObject libCfg;
    public Boolean pubFlag;
    public Integer interval;
    public Integer usage;
    public Integer datasetId;
    public String datasetName;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

