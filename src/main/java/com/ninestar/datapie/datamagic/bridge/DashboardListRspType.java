package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;

import java.sql.Timestamp;

//camel style to match with column name of DB
public class DashboardListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String type;
    public Integer pageCount;
    public JSONArray pages;
    public Boolean pubFlag;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

