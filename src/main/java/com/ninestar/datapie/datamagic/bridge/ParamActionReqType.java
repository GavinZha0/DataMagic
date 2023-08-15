package com.ninestar.datapie.datamagic.bridge;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.sql.Date;

//camel style to match with column name of DB
public class ParamActionReqType {
    public Integer id; // for update only
    public String name;
    public String desc;
    public String group;
    public String module;
    public String type;
    public String value;
    public String previous;
}