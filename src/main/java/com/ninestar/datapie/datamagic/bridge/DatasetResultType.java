package com.ninestar.datapie.datamagic.bridge;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.ColumnField;
import lombok.Data;

import java.util.List;

@Data
public class DatasetResultType {
    public UniformResponseCode code;
    public Integer total;
    public List<Object[]> records;
    public List<ColumnField> columns;
}

