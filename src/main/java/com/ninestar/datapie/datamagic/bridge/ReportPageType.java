package com.ninestar.datapie.datamagic.bridge;


public class ReportPageType {
    public Integer id;
    public String name;
    public String layout;
    public Long[] dataviews;
    public FilterType[] filter;


    public class FilterType{
        public String label;
        public String value;
    }
}

