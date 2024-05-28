package com.ninestar.datapie.datamagic.bridge;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.sql.Date;

//camel style to match with column name of DB
public class OrgActionReqType {
    public Integer id; // for update only
    public Integer pid;
    @NotNull(message = "name is not nullable")
    public String name;
    public String desc;
    public String logo;
    public Boolean active;
    public Boolean deleted;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    public Date expDate;
}