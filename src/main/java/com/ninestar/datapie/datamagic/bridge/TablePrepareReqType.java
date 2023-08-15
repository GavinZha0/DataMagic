package com.ninestar.datapie.datamagic.bridge;

import java.util.ArrayList;
import java.util.List;

public class TablePrepareReqType {
    public List<FieldInfoType> fileInfo = new ArrayList<>();
    public DbConfigType dbConfig = new DbConfigType();
    public FileSrvConfigType fsConfig = new FileSrvConfigType();

    public static class FieldInfoType {
        public String name;
        public String type;
        public String encoding;
        public Boolean header;
        public String quoteChar;
        public String delimiter;
        public Integer total;
    }

    public static class DbConfigType {
        public boolean enabled;
        public String[] nullText;
        public String[] trueText;
        public String[] falseText;
        public Integer sourceId;
        public String table;
        public Boolean overwrite;
        public List<FieldMapType> fieldMap = new ArrayList<>();
    }

    public static class FieldMapType {
        public Integer id; // index
        public String column;
        public String field;
        public  String type;
    }

    public static class FileSrvConfigType {
        public boolean enabled;
        public String folderName;
        public String fileName;
    }
}

