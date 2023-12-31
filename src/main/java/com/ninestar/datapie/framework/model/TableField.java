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

// Basic field definition of table

@Data
public class TableField {
    private Integer id;
    private String name;
    private String type;
    private Boolean locked;

    public TableField(Integer idx, String name, String type) {
        this.id = idx;
        this.name = name;
        this.type = type.toUpperCase();
        this.locked = false;
    }
}
