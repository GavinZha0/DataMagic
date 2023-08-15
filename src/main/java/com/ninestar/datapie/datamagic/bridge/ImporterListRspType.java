package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class ImporterListRspType {
    public Integer id;
    public JSONArray files;
    public String type;
    public Integer records; // imported records
    public Integer rows; // total records fo all files
    public String sourceName;
    public String tableName;


    public JSONObject attrs;
    public JSONArray fields;
    public JSONObject config;
    public Boolean overwrite;
    public String status;
    public String detail;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

