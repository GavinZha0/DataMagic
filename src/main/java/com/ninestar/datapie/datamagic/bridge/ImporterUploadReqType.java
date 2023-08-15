package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.framework.model.ColumnField;

import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class ImporterUploadReqType {
    public String type;
    public Integer source;
    public String table;
    public Integer rows; // total records
    public Boolean overwrite;
    public List<FileNameType> fileNames = new ArrayList<>();
    public FileAttrType fileAttrs;
    public List<ColFieldType> colFields = new ArrayList<>();
    public DbConfigType dbConfig;

    public static class FileNameType {
        public String name;
        public Integer size;
        public Integer rows;
    }

    public static class FileAttrType {
        public Boolean header;
        public String encoding;
        public String quote;
        public String delimiter;
        public String timezone;
        public String timeFormat;
        public String dateFormat;
        public String tsFormat;
    }
    public static class ColFieldType {
        public Integer key;
        public Boolean ignore;
        public String title;
        public String alias;
        public String type;
        public String javaType;
        public String precision;
        public String formula;
    }

    public static class DbConfigType {
        public String ts;
        public List<String> nullMap = new ArrayList<>();
        public List<String> trueMap = new ArrayList<>();
        public List<String> falseMap = new ArrayList<>();
    }
}

