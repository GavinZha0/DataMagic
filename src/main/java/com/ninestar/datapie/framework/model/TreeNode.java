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

// Tree list for Tree component of antd vue

@Data
public class TreeNode {
    private Integer id;
    private String name;
    private String value;
    private Boolean selectable;
    private Boolean isLeaf;

    private List<TreeNode> children = new ArrayList<>();

    public TreeNode(Integer id, String name, String value, Boolean selectable, Boolean isLeaf) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.selectable = selectable;
        this.isLeaf = isLeaf;
    }
}
