package com.ninestar.datapie.datamagic.bridge;

import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class TableListReqType{
    public PageType page;
    public SorterType sorter;
    public FilterType filter;
    public SearchType search;


    public class PageType {
        public Integer current;
        public Integer pageSize;
    }

    public static class SorterType {
        public String fields[];
        public String orders[];
    }

    public static class FilterType {
        public List<String> fields = new ArrayList<>();
        public List<String[]> values = new ArrayList<>();
    }

    public class FilterType1111 {
        public String[] fields;
        public String[][] values;
    }

    public class SearchType {
        public String value;
        public String[] fields;
    }
}
