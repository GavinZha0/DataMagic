package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class AiModelActionReqType {
    public Integer id;
    public String name;
    public String description;
    public String category;
    public String type;
    public List<String> tags = new ArrayList<>();
    public String version;
    public String network;
    public String framework;
    public String frameVer;
    public String trainset;
    public List<String> files = new ArrayList<>();
    public JSONArray input;
    public JSONArray output;
    public JSONArray eval;
    public Integer score;
    public String price;
    public Integer modelId;
    public JSONObject detail;
    public String weblink;
    public Boolean pubFlag;
}

