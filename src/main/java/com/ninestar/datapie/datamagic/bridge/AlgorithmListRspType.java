package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class AlgorithmListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public List<String> tags = new ArrayList<>();
    public String category;
    public String algoName;
    public JSONObject dataCfg;
    public JSONObject trainCfg;
    public String datasetName;
    public String srcCode;
    public Boolean pubFlag;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

