package com.ninestar.datapie.datamagic.bridge;
import lombok.Data;

@Data
public class DatasetFieldType {
    public Integer id;
    public String name;
    public String type;
    public String alias;
    public Boolean metrics;
    public Boolean hidden;
    public String filter;
    public String orderDir;
    public Integer orderPri;
}

