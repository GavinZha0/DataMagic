package com.ninestar.datapie.datamagic.bridge;

import com.ninestar.datapie.datamagic.utils.SqlUtils;

import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class DatasetExeSqlReqType {
    public Integer id; // source id
    public String sql;
    public List<SqlUtils.VariableType> variable = new ArrayList<SqlUtils.VariableType>();
    public List<SqlUtils.FieldType> fields = new ArrayList<SqlUtils.FieldType>();
    public Integer limit;

    public static class VariableType {
        public String name;
        public String type;
        public String value;
    }

    public static class FieldType {
        public String name;
        public String type;
        public String alias;
        public boolean hidden;
        public String filter;
        public Integer order;
    }
}

