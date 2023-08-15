package com.ninestar.datapie.datamagic.bridge;

import java.util.List;

//camel style to match with column name of DB
public class RoleActionReqType {
    public Integer id; // for update only
    public String name;
    public String desc;
    public Boolean active;
    public Integer orgId;
    public List<Integer> userIds;
}

