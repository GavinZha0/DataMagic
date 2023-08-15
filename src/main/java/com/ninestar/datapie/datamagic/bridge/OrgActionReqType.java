package com.ninestar.datapie.datamagic.bridge;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.sql.Date;

//camel style to match with column name of DB
public class OrgActionReqType {
    public Integer id; // for update only
    public Integer pid;
    public String name;
    public String desc;
    public String logo;
    public Boolean active;
    public Boolean deleted;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    public Date expDate;
}