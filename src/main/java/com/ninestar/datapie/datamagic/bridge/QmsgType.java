package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONObject;
import lombok.Data;

@Data
public class QmsgType {
    public Integer userId;
    public JSONObject payload;
}

