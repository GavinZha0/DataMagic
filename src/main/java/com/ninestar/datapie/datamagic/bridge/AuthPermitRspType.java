package com.ninestar.datapie.datamagic.bridge;

import java.util.ArrayList;
import java.util.List;

public class AuthPermitRspType {
    public Integer id;
    public Integer pid;
    public String name;
    public String title;
    public String icon;
    public String url;
    public String component;
    public String redirect;
    public Boolean dySubRpt;
    public String permit;
    public List<AuthPermitRspType> children = new ArrayList<AuthPermitRspType>();
}
