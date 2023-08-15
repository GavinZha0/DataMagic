package com.ninestar.datapie.datamagic.bridge;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class RoleListRspType {
    public Integer id;
    public String name;
    public String desc;
    public Boolean active;
    public Integer orgId;
    public String orgName;
    public List<Integer> userIds = new ArrayList<>();
    public Integer userCount;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

