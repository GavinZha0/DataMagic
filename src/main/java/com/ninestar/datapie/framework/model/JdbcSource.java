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

//JDBC datasource definition

@Data
public class JdbcSource {

    private Integer id;

    private String name;

    private String type;

    private String url;

    private String params;

    private String username;

    private String password;

    public JdbcSource(Integer id, String name, String type, String url, String username, String password) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.url = url;
        this.username = username;
        this.password = password;
    }
}
