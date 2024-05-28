package com.ninestar.datapie.datamagic.bridge;

import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Timestamp;
import java.sql.Date;

//camel style to match with column name of DB
@Schema(description = "User list response type")
public class UserListRspType {
    public Integer id;
    public String name;
    public String password;
    public String realname;
    public String desc;
    public String avatar;
    public String email;
    public String phone;
    public String social;
    public Integer orgId;
    public String orgName;
    public String[] roles;
    public Boolean active;
    public Date expDate;
    public String createdBy;
    public Timestamp createdAt;
    public String updatedBy;
    public Timestamp updatedAt;
}

