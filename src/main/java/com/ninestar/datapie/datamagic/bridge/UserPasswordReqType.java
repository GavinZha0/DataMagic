package com.ninestar.datapie.datamagic.bridge;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.sql.Date;

//camel style to match with column name of DB
public class UserPasswordReqType {
    public Integer id;
    public String oldPwd;
    public String newPwd;
}

