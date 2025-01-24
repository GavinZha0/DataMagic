package com.ninestar.datapie.datamagic.bridge;

import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class AiModelActionReqType {
    public Integer id;
    public String name;
    public String desc;
    public String area;
    public List<String> tags = new ArrayList<>();
    public Integer rate;
    public String price;
    public Integer algoId;
    public Integer datasetId;
    public Boolean pubFlag;
    public String runId;
    public Integer version;
    public String deployTo;
    public String endpoint;
}

