package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class DatareportListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String group;
    public String type;
    public Integer pageCount;
    public JSONArray pages;
    public Boolean pubFlag;
    public Integer menuId;
    public String menuName;
    public String menuTitle;
    public List<Integer> viewIds = new ArrayList<>();
    public Boolean publishPub;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

