/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package com.ninestar.datapie.framework.model;

import cn.hutool.core.util.StrUtil;
import com.ninestar.datapie.framework.consts.SqlColumn;
import lombok.Data;

// Basic field definition of column

@Data
public class ColumnField {
    private Integer id; // idx in table
    private String name; // column name
    private String type; // data type
    private Integer size; // length
    private Boolean primary; // primary key or not
    private Boolean metrics; // metrics or dim for app level using

    public ColumnField(Integer idx, String name, String type) throws Exception {
        if(idx!=null){
            this.id = idx;
        }
        this.name = name;
        this.type = SqlColumn.toJsType(type);
    }


    public ColumnField(String idx, String name, String type, String size, Boolean primary) throws Exception {
        if(!StrUtil.isEmpty(idx)){
            this.id = Integer.parseInt(idx);
        }

        if(!StrUtil.isEmpty(size)){
            this.size = Integer.parseInt(size);
        }

        this.name = name;
        this.type = SqlColumn.toJsType(type);
        this.primary = primary;
        this.metrics = false;
    }
}
