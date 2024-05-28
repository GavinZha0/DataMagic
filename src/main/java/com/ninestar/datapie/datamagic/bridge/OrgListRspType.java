package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
@Schema(description = "Org list response type")
public class OrgListRspType {
    public Integer id;
    public String name;
    public String desc;
    public String logo;
    public Date expDate;
    public Boolean active;
    public boolean deleted;
    public Integer usage;

    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}


