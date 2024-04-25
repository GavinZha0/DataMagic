package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONObject;
import java.sql.Timestamp;

//camel style to match with column name of DB
public class MlEdaListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public Integer datasetId;
    public String datasetName;
    public JSONObject config;
    public Boolean pubFlag;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

