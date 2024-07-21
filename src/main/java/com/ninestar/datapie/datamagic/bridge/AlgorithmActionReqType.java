package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

//camel style to match with column name of DB
public class AlgorithmActionReqType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String framework;
    public String category;
    public String algoName;
    public Integer datasetId;
    public JSONObject attr;
    public JSONObject config;
    public String srcCode;
    public Boolean pubFlag;
}

