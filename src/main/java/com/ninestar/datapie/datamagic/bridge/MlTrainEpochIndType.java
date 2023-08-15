package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class MlTrainEpochIndType {
    public String stage; // train or eval
    public Integer epoch;
    public Integer numEpoch;
    public Integer iterator;
    public Integer numIterator;
    public Integer progress;
    public Float accuracy;
    public Float score;
    public Float precision;
    public Float recall;
    public Float f1;
    public Timestamp startAt;
}

