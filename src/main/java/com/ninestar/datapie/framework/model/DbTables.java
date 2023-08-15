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

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// DB Tables

@Data
public class DbTables {
    private String dbName;
    private String dbType;
    private List<TableField> tables;

    public DbTables(String dbName, String dbType, List<TableField> tables) {
        this.dbName = dbName;
        this.dbType = dbType;
        this.tables = tables;
    }
}
