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

package com.ninestar.datapie.framework.consts;

import cn.hutool.core.util.StrUtil;
import com.ninestar.datapie.framework.utils.DateUtils;
import org.apache.commons.lang3.StringUtils;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.math.BigDecimal;
import java.sql.SQLException;

import static com.ninestar.datapie.framework.consts.Consts.EMPTY;

public enum SqlColumn {
    DEFAULT("", "String", "string"),
    TINYINT("TINYINT", "Short", "number"),
    SMALLINT("SMALLINT", "Short", "number"),
    INT("INT", "Integer", "number"),
    INTEGER("INTEGER", "Integer", "number"),
    BIGINT("BIGINT", "Long", "number"),
    DECIMAL("DECIMAL", "BigDecimal", "number"),
    NUMERIC("NUMERIC", "BigDecimal", "number"),
    REAL("REAL", "Float", "number"),
    FLOAT("FLOAT", "Float", "number"),
    DOUBLE("DOUBLE", "Double", "number"),
    CHAR("CHAR", "String", "string"),
    VARCHAR("VARCHAR", "String", "string"),
    NVARCHAR("NVARCHAR", "String", "string"),
    LONGVARCHAR("LONGVARCHAR", "String", "string"),
    LONGNVARCHAR("LONGNVARCHAR", "String", "string"),
    TINYTEXT("TINYTEXT", "String", "string"),
    TEXT("TEXT", "String", "string"),
    LONGTEXT("LONGTEXT", "String", "string"),
    BOOLEAN("BOOLEAN", "Boolean", "boolean"),
    BIT("BIT", "Boolean", "boolean"),
    BINARY("BINARY", "Bytes", "bytes"),
    VARBINARY("VARBINARY", "Bytes", "bytes"),
    LONGVARBINARY("LONGVARBINARY", "Bytes", "bytes"),
    TIME("TIME", "Time", "timestamp"),
    DATE("DATE", "Date", "timestamp"),
    DATETIME("DATETIME", "DateTime", "timestamp"),
    TIMESTAMP("TIMESTAMP", "Timestamp", "timestamp"),
    BLOB("BLOB", "Blob", "blob"),
    CLOB("CLOB", "Clob", "clob");

    private String name;
    private String javaType;
    private String jsType;

    SqlColumn(String name, String javaType, String jsType) {
        this.name = name;
        this.javaType = javaType;
        this.jsType = jsType;
    }

    public static Object formatValue(String type, String value) throws Exception {
        type = type.toUpperCase();
        for (SqlColumn sqlTypeEnum : values()) {
            if (sqlTypeEnum.name.equals(type)) {
                Object object = null;
                try {
                    object = s2dbValue(type, value);
                } catch (Exception e) {
                    throw new Exception(e.toString() + ":[" + type + ":" + value + "]");
                }
                return object;
            }
        }
        return value;
    }

    public static String toJavaType(String type) throws Exception {
        if(StrUtil.isEmpty(type)){
            return SqlColumn.CHAR.javaType;
        }
        type = type.toUpperCase();
        int i = type.indexOf("(");
        if (i > 0) {
            type = type.substring(0, i);
        }
        for (SqlColumn sqlTypeEnum : values()) {
            if (sqlTypeEnum.name.equals(type)) {
                return sqlTypeEnum.javaType;
            }
        }
        return null;
    }

    public static String toJsType(String type) throws Exception {
        type = type.toUpperCase();
        int i = type.indexOf("(");
        if (i > 0) {
            type = type.substring(0, i);
        }
        for (SqlColumn sqlTypeEnum : values()) {
            if (sqlTypeEnum.name.equals(type)) {
                return sqlTypeEnum.jsType;
            }
        }
        return null;
    }

    private static Object s2dbValue(String type, String value) throws Exception {
        if (StringUtils.isEmpty(value)) {
            return null;
        }
        Object result = value.trim();
        switch (type.toUpperCase()) {
            case "TINYINT":
            case "SMALLINT":
                result = Short.parseShort(value.trim());
                break;

            case "INT":
            case "INTEGER":
                result = Integer.parseInt(value.trim());
                break;

            case "BIGINT":
                result = Long.parseLong(value.trim());
                break;

            case "DECIMAL":
            case "NUMERIC":
                if (EMPTY.equals(value.trim())) {
                    result = new BigDecimal("0.0").stripTrailingZeros();
                } else {
                    result = new BigDecimal(value.trim()).stripTrailingZeros();
                }
                break;

            case "FLOAT":
            case "REAL":
                result = Float.parseFloat(value.trim());
                break;

            case "DOUBLE":
                result = Double.parseDouble(value.trim());
                break;

            case "CHAR":
            case "VARCHAR":
            case "NVARCHAR":
            case "LONGVARCHAR":
            case "LONGNVARCHAR":
            case "TEXT":
                result = value.trim();
                break;

            case "BIT":
            case "BOOLEAN":
                result = Boolean.parseBoolean(value.trim());
                break;

            case "BINARY":
            case "VARBINARY":
            case "LONGVARBINARY":
                result = value.trim().getBytes();
                break;

            case "DATE":
                try {
                    result = DateUtils.toDate(value.trim());
                } catch (Exception e) {
                    throw new Exception(e.getMessage());
                }
                break;
            case "DATETIME":
                try {
                    result = DateUtils.toDateTime(value.trim());
                } catch (Exception e) {
                    throw new Exception(e.getMessage());
                }
                break;
            case "TIMESTAMP":
                try {
                    result = DateUtils.toTimestamp(value.trim());
                } catch (Exception e) {
                    throw new Exception(e.getMessage());
                }
                break;

            case "BLOB":
                try {
                    result = new SerialBlob(value.trim().getBytes());
                } catch (SQLException e) {
                    throw new Exception(e.getMessage());
                }
                break;
            case "CLOB":
                try {
                    result = new SerialClob(value.trim().toCharArray());
                } catch (SQLException e) {
                    throw new Exception(e.getMessage());
                }
                break;
            default:
                result = value.trim();
        }
        return result;
    }
}
