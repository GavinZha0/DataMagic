package com.ninestar.datapie.datamagic.bridge;

import cn.hutool.json.JSONObject;
import lombok.Data;

@Data
public class QmsgType {
    private Integer uid;
    private Integer code;
    private String msg;
    private Object data;
}

