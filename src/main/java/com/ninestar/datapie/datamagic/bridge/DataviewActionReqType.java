package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

//camel style to match with column name of DB
public class DataviewActionReqType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String type;
    public JSONArray dim;
    public JSONArray relation;
    public JSONArray location;
    public JSONArray metrics;
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
    public Integer datasetId;
}

