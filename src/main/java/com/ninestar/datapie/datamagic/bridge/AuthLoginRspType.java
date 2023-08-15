package com.ninestar.datapie.datamagic.bridge;

import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class AuthLoginRspType {
    public Integer id;
    public String name;
    public String realname;
    public String avatar;
    public Integer orgId;
    public String orgName;
    public List<Integer> roleId = new ArrayList<>();
    public List<String> roleName = new ArrayList<>();
}

