package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONObject;
import com.ninestar.datapie.framework.model.ColumnField;

import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class DataviewLoadRspType {
    public Integer id;
    public String name;
    public String group;
    public String type;
    public List<String> dim = new ArrayList<>();
    public List<String> metrics = new ArrayList<>();
    public String libName;
    public String libVer;
    public JSONObject libCfg;
    public List<Column> columns = new ArrayList<>();
    public List<Object[]> records = new ArrayList<>();

    public static class Column {
        public String name;
        public String type;
    }
}
