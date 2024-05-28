package com.ninestar.datapie.datamagic.bridge;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.sql.Date;

//camel style to match with column name of DB
@Schema(description = "User action request type")
public class UserActionReqType {
    public Integer id; // for update only

    @NotNull(message = "user name is not nullable")
    @Schema(description = "user name", requiredMode = Schema.RequiredMode.REQUIRED)
    public String name;

    public String password;
    public String desc;
    public String realname;
    public String avatar;
    public String email;
    public String phone;
    public String social;
    public String orgName;
    public String[] roleNames;
    public Boolean active;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    public Date expDate;
    public Integer part;
}

