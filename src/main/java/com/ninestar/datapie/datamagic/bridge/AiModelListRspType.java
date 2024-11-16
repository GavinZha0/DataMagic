package com.ninestar.datapie.datamagic.bridge;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class AiModelListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String area;
    public List<String> tags = new ArrayList<>();
    public Integer algoId;
    public String algoName;
    public Integer version;
    public String runId;
    public Float rate;
    public String eval;
    public String price;
    public String deployTo;
    public String endpoint;
    public Integer usage;
    public Boolean pubFlag;
    public Object metrics;
    public Object schema;
    // 0:idle; 1:serving; 2:exception; 3:unknown;
    public Integer status;
    public String trainedBy;
    public Timestamp trainedAt;
    public String createdBy;
    public Timestamp createdAt;
    public String deployedBy;
    public Timestamp deployedAt;
}

