package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.framework.model.ColumnField;

import java.util.ArrayList;
import java.util.List;

//camel style to match with column name of DB
public class DatareportPageRspType {
    public Integer id;
    public String title;
    public String aspectRatio; // '16x9' or 'adaptive'
    public String layout; // '3x2', 'zoom-left', 'free' or 'scalable'
    public Boolean showTitle; // view title
    public Boolean showToolbar; // view toolbar
    public Boolean showBorder; // view border
    public JSONArray filter; // report filter
    public JSONArray grids;
    public List<DatareportViewType> views = new ArrayList<>();

    public static class DatareportPageType {
        public Integer id;
        public String title;
        public String aspectRatio; // '16x9' or 'adaptive'
        public String layout; // '3x2', 'zoom-left', 'free' or 'scalable'
        public Boolean showTitle; // view title
        public Boolean showToolbar; // view toolbar
        public Boolean showBorder; // view border
        public JSONArray grids;
    }
    public static class DatareportGridType {
        public Integer id;
        public Integer i;
        public Integer x;
        public Integer y;
        public Integer w;
        public Integer h;
        public String type;
        public String placeholder;
    }
    public static class DatareportViewType {
        public Integer id;
        public String type;
        public String libName;
        public Integer intervalMin;
        public JSONObject libCfg;
        public List<ColumnField> recordField = new ArrayList<>();
        public List<Object[]> records = new ArrayList<>();
    }
}
