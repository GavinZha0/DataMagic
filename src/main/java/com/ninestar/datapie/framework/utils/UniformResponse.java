package com.ninestar.datapie.framework.utils;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.Set;


@Data
public class UniformResponse {

    private Integer code;
    private String msg;
    private Object data;
    protected UniformResponse(){}

    public static UniformResponse ok() {
        UniformResponse r = new UniformResponse();
        r.setCode(UniformResponseCode.SUCCESS.getCode());
        r.setMsg(UniformResponseCode.SUCCESS.getMsg());
        return r;
    }

    public static UniformResponse error() {
        UniformResponse r = new UniformResponse();
        r.setCode(UniformResponseCode.EXCEPTION.getCode());
        r.setMsg(UniformResponseCode.EXCEPTION.getMsg());
        return r;
    }

    public static UniformResponse error(String msg) {
        UniformResponse r = new UniformResponse();
        r.setCode(UniformResponseCode.EXCEPTION.getCode());
        r.setMsg(msg);
        return r;
    }

    public static UniformResponse error(UniformResponseCode err) {
        UniformResponse r = new UniformResponse();
        r.setCode(err.getCode());
        r.setMsg(err.getMsg());
        return r;
    }


    public UniformResponse data(Object data) {
        if(data == null){
            return this;
        }

        if((data instanceof List) || (data instanceof Set)){
            //put list into records
            JSONObject jsonData = new JSONObject();
            jsonData.set("records", data);
            this.setData(jsonData);
        }
        else if(data.getClass().getName().contains("org.springframework.data.domain.PageImpl")){
            // convert jpa page to custom struct
            JSONObject jsonData = JSONUtil.parseObj(data);
            jsonData.set("total", jsonData.get("totalElements"));
            jsonData.set("current", jsonData.get("number"));
            jsonData.set("records", jsonData.get("content"));

            jsonData.remove("totalElements");
            jsonData.remove("number");
            jsonData.remove("content");
            jsonData.remove("last");
            jsonData.remove("numberOfElements");
            jsonData.remove("size");
            jsonData.remove("totalPages");
            jsonData.remove("pageable");
            jsonData.remove("sort");
            jsonData.remove("first");
            jsonData.remove("empty");
            this.setData(jsonData);
        }
        else if(data.getClass().getName().contains("com.baomidou.mybatisplus.extension.plugins.pagination.Page")){
            // convert Mybatis plus page to custom struct
            JSONObject jsonData = JSONUtil.parseObj(data);
            jsonData.remove("size");
            jsonData.remove("orders");
            jsonData.remove("optimizeCountSql");
            jsonData.remove("isSearchCount");
            jsonData.remove("pageable");
            jsonData.remove("hitCount");
            jsonData.remove("countId");
            jsonData.remove("maxLimit");
            this.setData(jsonData);
        }
        else if(data.getClass().getName().contains("DbTables")){
            // rename tables to records
            JSONObject jsonData = JSONUtil.parseObj(data);
            jsonData.set("records", jsonData.get("tables"));
            jsonData.remove("tables");
            this.setData(jsonData);
        }
        else if(data.getClass().getName().contains("TableColumns")){
            // rename tables to records
            JSONObject jsonData = JSONUtil.parseObj(data);
            jsonData.set("records", jsonData.get("columns"));
            jsonData.remove("columns");
            this.setData(jsonData);
        }
        else{
            this.setData(data);
        }
        return this;
    }

    public UniformResponse data(Object data, Object metadata, Integer totalRowCount){
        if(data == null){
            return this;
        }

        if((data instanceof List) || (data instanceof Set)){
            JSONObject jsonData = new JSONObject();

            if(totalRowCount!=null){
                jsonData.set("total", totalRowCount);
            }
            else{
                jsonData.set("total", ((Collection<?>) data).size());
            }
            //put list into records
            jsonData.set("records", data);
            // put columns into metadata. It will be removed if a field is null.
            jsonData.set("metadata", metadata);
            this.setData(jsonData);
        }
        else if(data.getClass().getName().contains("org.springframework.data.domain.PageImpl")){
            // convert jpa page to custom struct
            JSONObject jsonData = JSONUtil.parseObj(data);
            jsonData.set("total", jsonData.get("totalElements"));
            jsonData.set("current", jsonData.get("number"));
            jsonData.set("records", jsonData.get("content"));
            jsonData.set("metadata", metadata);

            jsonData.remove("totalElements");
            jsonData.remove("number");
            jsonData.remove("content");
            jsonData.remove("last");
            jsonData.remove("numberOfElements");
            jsonData.remove("size");
            jsonData.remove("totalPages");
            jsonData.remove("pageable");
            jsonData.remove("sort");
            jsonData.remove("first");
            jsonData.remove("empty");
            this.setData(jsonData);
        }
        else if(data.getClass().getName().contains("com.baomidou.mybatisplus.extension.plugins.pagination.Page")){
            // convert Mybatis plus page to custom struct
            JSONObject jsonData = JSONUtil.parseObj(data);
            jsonData.set("metadata", metadata);
            jsonData.remove("size");
            jsonData.remove("orders");
            jsonData.remove("optimizeCountSql");
            jsonData.remove("isSearchCount");
            jsonData.remove("pageable");
            jsonData.remove("hitCount");
            jsonData.remove("countId");
            jsonData.remove("maxLimit");
            this.setData(jsonData);
        }
        else if(data.getClass().getName().contains("DbTables")){
            // rename tables to records
            JSONObject jsonData = JSONUtil.parseObj(data);
            jsonData.set("records", jsonData.get("tables"));
            jsonData.set("metadata", metadata);
            jsonData.remove("tables");
            this.setData(jsonData);
        }
        else if(data.getClass().getName().contains("TableColumns")){
            // rename tables to records
            JSONObject jsonData = JSONUtil.parseObj(data);
            jsonData.set("records", jsonData.get("columns"));
            jsonData.set("metadata", metadata);
            jsonData.remove("columns");
            this.setData(jsonData);
        }
        else{
            this.setData(data);
        }
        return this;
    }
}
