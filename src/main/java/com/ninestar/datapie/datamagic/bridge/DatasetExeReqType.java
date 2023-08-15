package com.ninestar.datapie.datamagic.bridge;

import com.ninestar.datapie.datamagic.utils.SqlUtils;

import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class DatasetExeReqType {
    public Integer id; // dataset id
    public Integer limit;
    public Boolean update;
}

