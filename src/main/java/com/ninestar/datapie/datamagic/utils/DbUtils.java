package com.ninestar.datapie.datamagic.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.bridge.ImporterUploadReqType;
import com.ninestar.datapie.framework.common.DsHolder;
import com.ninestar.datapie.framework.consts.Consts;
import com.ninestar.datapie.framework.consts.DatabaseType;
import com.ninestar.datapie.framework.consts.SqlColumn;
import com.ninestar.datapie.framework.consts.SqlType;
import com.ninestar.datapie.framework.model.*;
import com.ninestar.datapie.framework.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import cn.hutool.core.codec.Base64;

import static java.sql.Types.*;

@Slf4j
@Component
@Scope("prototype")
public class DbUtils {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String TABLE = "TABLE";

    private final String VIEW = "VIEW";

    private final String[] TABLE_TYPES = new String[]{TABLE, VIEW};

    private final String TABLE_NAME = "TABLE_NAME";

    private final String TABLE_TYPE = "TABLE_TYPE";

    private static DatabaseType dataTypeEnum;

    private DsHolder dsHolder = new DsHolder();

    private Integer progress = 0;

    /*
    * add new datasource
    */
    public void add(JdbcSource source) {
        // Password decryption
        String password = new String(Base64.decode(source.getPassword()));
        dsHolder.addSource(source.getId(), source.getName(), source.getType(), source.getUrl(), source.getParams(), source.getUsername(), password);
    }

    /*
     * add new datasource
     */
    public void add(Integer id, String sourceName, String type, String jdbcUrl, String jsonParamStr, String username, String password) {
        // convert json parameter to string
        String paramStr = "";
        if(!StrUtil.isEmpty(jsonParamStr)){
            JSONArray params = new JSONArray(jsonParamStr);
            if(params!=null && params.size()>0) {
                List<String> paramList = new ArrayList<String>();
                for (Object param : params) {
                    JSONObject obj = JSONUtil.parseObj(param);
                    String name = obj.getStr("name");
                    String value = obj.getStr("value");
                    paramList.add(name + "=" + value);
                }
                paramStr = String.join("&", paramList);
            }
        }

        dsHolder.addSource(id, sourceName, type, jdbcUrl, paramStr, username, password);
    }

    public boolean isSourceExist(Integer id){
        return dsHolder.isSourceExist(id);
    }

    public Integer getProgress(){
        return progress;
    }

    public void setProgress(Integer value){progress = value;}

    /**
     * Get databases
     * @param id: datasource id
     * @return
     * @throws Exception
     */
    public List<String> getDatabases(Integer id) throws Exception {
        List<String> dbList = null;
        Connection connection = null;
        try {
            connection = dsHolder.getConnection(id);
            if (null == connection) { return null; }

            dbList = new ArrayList<>();
            String catalog = connection.getCatalog();
            if (!StringUtils.isEmpty(catalog)) {
                // return current database if it is in connection (jdbc url)
                dbList.add(catalog);
            } else {
                DatabaseMetaData metaData = connection.getMetaData();
                ResultSet rs = metaData.getCatalogs();
                while (rs.next()) {
                    dbList.add(rs.getString("TABLE_CAT"));
                }
            }

        } catch (Exception e) {
            log.error(e.toString(), e);
            return dbList;
        } finally {
            connection.close();
        }
        return dbList;
    }
    /**
     * Get database tables
     * @param id: datasource id
     * @param dbName: database name
     * @return
     * @throws Exception
     */

    // there is a issue
    // should I close connection at the end?
    // the connections can't be reused even I close them at the end.
    // Running 'show processlist' can see the connections in Mysql db
    // the parameters(idle-timeout, max-lifetime...) don't work. Why?
    public DbTables getDbTables(Integer id, String dbName) throws Exception {
        DbTables dbTables = null;
        List<TableField> tableList = null;
        Connection connection = null;
        ResultSet tables = null;
        String databaseName = dbName;

        try {
            connection = dsHolder.getConnection(id);
            if (null == connection || connection.isClosed()) { return null; }

            if(StrUtil.isEmpty(databaseName)){
                // get database name from jdbc url
                databaseName = connection.getCatalog();
            }

            String schema = connection.getSchema();
            DatabaseMetaData metaData = connection.getMetaData();
            tables = metaData.getTables(databaseName, getDBSchemaPattern(id, schema), "%", TABLE_TYPES);
            if (null == tables) { return null; }

            int i = 0;
            tableList = new ArrayList<>();
            while (tables.next()) {
                tableList.add(new TableField(i, tables.getString(TABLE_NAME), tables.getString("TABLE_TYPE")));
                i++;
            }
            dbTables = new DbTables(databaseName, "", tableList);
        } catch (Exception e) {
            log.error(e.toString(), e);
            return dbTables;
        } finally {
            connection.close();
        }
        return dbTables;
    }



    private String getDBSchemaPattern(Integer id, String schema) throws Exception {
        if (dataTypeEnum == null) {
            return null;
        }
        String schemaPattern = null;
        switch (dataTypeEnum) {
            case SQLSERVER:
                schemaPattern = "dbo";
                break;
            case CLICKHOUSE:
            case PRESTO:
                if (!StringUtils.isEmpty(schema)) {
                    schemaPattern = schema;
                }
                break;
            default:
                break;
        }
        return schemaPattern;
    }

    /**
     * Get table fields (columns)
     * @param id: datasource id
     * @param dbName: database name
     * @param tableName: table name
     * @param tableName
     * @return
     * @throws Exception
     */
    public TableColumns getTableColumns(Integer id, String dbName, String tableName) throws Exception {
        ResultSet rs = null;
        List<String> primaryKeys = new ArrayList<>();
        List<ColumnField> columnList = new ArrayList<>();
        TableColumns tableColumns = null;
        Connection connection = null;
        String databaseName = dbName;

        try {
            connection = dsHolder.getConnection(id);
            if (null == connection) { return null; }

            if(StrUtil.isEmpty(databaseName)){
                // get database name from jdbc url
                databaseName = connection.getCatalog();
            }

            DatabaseMetaData metaData = connection.getMetaData();
            // get primary keys
            rs = metaData.getPrimaryKeys(databaseName, null, tableName);
            if (rs != null) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }

            // get columns
            rs = metaData.getColumns(databaseName, null, tableName, "%");
            if (rs != null) {
                while (rs.next()) {
                    //Integer tt = rs.getType();
                    //String aaa = rs.getString("DATA_TYPE");
                    Boolean primary = false;
                    primary = primaryKeys.contains(rs.getString("COLUMN_NAME"));
                    columnList.add(new ColumnField(rs.getString("ORDINAL_POSITION"),
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getString("COLUMN_SIZE"),
                            primary));
                }
            }

            tableColumns = new TableColumns(tableName, columnList);

        } catch (SQLException e) {
            log.error(e.toString(), e);
            throw new Exception(e.getMessage() + ", jdbcUrl=" + id);
        }
        return tableColumns;
    }


    /**
     * check database table exist or not
     *
     * @param tableName
     * @return
     * @throws Exception
     */
    public boolean isTableIsExist(Integer id, String tableName) throws Exception {
        boolean result = false;
        Connection connection = null;
        ResultSet rs = null;
        try {
            connection = dsHolder.getConnection(id);
            if (null != connection) {
                rs = connection.getMetaData().getTables(null, null, tableName, null);
                if (null != rs) {
                    while (rs.next()) {
                        String tName = rs.getString(TABLE_NAME);
                        if(tName.equalsIgnoreCase(tableName)){
                            result = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new Exception("Get connection meta data error, jdbcUrl=" + id);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    log.error("ResultSet close error", e);
                }
            }
        }
        return result;
    }

    /*
    * Create jdbc template based on specified datasource
    */
    public JdbcTemplate jdbcTemplate(Integer id) throws Exception {
        DataSource dataSource = dsHolder.getSource(id);
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
        }catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        //
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setDatabaseProductName("Source_"+id);
        jdbcTemplate.setFetchSize(500);
        return jdbcTemplate;
    }

    /*
     * Execute sql based on JdbcTemplate
     */
    public void execute(Integer id, String sql) throws Exception {
        try {
            jdbcTemplate(id).execute(sql);
        } catch (Exception e) {
            log.error(e.toString(), e);
            throw new Exception(e.getMessage());
        }
    }

    /*
     * Test connection
     */
    public String testConnection(Integer id) throws Exception {
        Boolean testResult = false;
        String dbVersion = null;
        Connection connection = null;
        try {
            connection = dsHolder.getConnection(id);
            if (null != connection) {
                testResult = true;
                DatabaseMetaData metaData = connection.getMetaData();
                dbVersion = metaData.getDatabaseProductVersion();
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        finally {
            if(id==99999){
                //remove test source
                dsHolder.removeSource(id);
            }
            return dbVersion;
        }
    }

    /*
     * Execute SQL and return result
     */
    public Boolean execute(Integer id, String sql, List<ColumnField> headers, List<Object[]> resultMap) throws Exception {
        if(id==null || StrUtil.isEmpty(sql)){
            return false;
        }

        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet result = null;
        ResultSetMetaData metaData = null;
        try {
            connection = dsHolder.getConnection(id);
            if (null == connection) {
                return false;
            }
            // run query
            pstmt = connection.prepareStatement(sql);
            result = pstmt.executeQuery();

            // get column names
            metaData = result.getMetaData();
            int colSize = metaData.getColumnCount();
            for(int i=0; i<colSize; i++){
                headers.add(new ColumnField(i, metaData.getColumnLabel(i+1), metaData.getColumnTypeName(i+1)));
            }

            // get values
            while (result.next()) {
                Object[] colArray = new Object[colSize];
                for(int j=0; j<colSize; j++){
                    String dataType = metaData.getColumnClassName(j+1);
                    switch (dataType){
                        case "java.lang.Integer":
                        {
                            colArray[j] = result.getInt(j+1);
                            break;
                        }
                        case "java.lang.Long":{
                            colArray[j] = result.getLong(j+1);
                            break;
                        }
                        case "java.lang.Float":{
                            colArray[j] = result.getFloat(j+1);
                            break;
                        }
                        case "java.lang.Double":{
                            colArray[j] = result.getDouble(j+1);
                            break;
                        }
                        case "java.lang.Date":{
                            colArray[j] = result.getDate(j+1);
                            break;
                        }
                        case "java.lang.Time":{
                            colArray[j] = result.getTime(j+1);
                            break;
                        }
                        case "java.lang.Timestamp":{
                            colArray[j] = result.getTimestamp(j+1);
                            break;
                        }
                        case "java.lang.Boolean":{
                            colArray[j] = result.getBoolean(j+1);
                            break;
                        }
                        default:{
                            colArray[j] = result.getString(j+1);
                            break;
                        }
                    }
                }
                resultMap.add(colArray);

            }
        }catch (Exception e){
            logger.error(e.getMessage());
            throw new Exception(e);
        } finally {
            pstmt.close();
            connection.close();
        }

        return true;
    }


    /*
     * Execute batch sql
     */
    public void executeBatch(Integer id, String sql, Set<ColumnField> headers, List<Map<String, Object>> datas) throws Exception {

        if (StringUtils.isEmpty(sql)) {
            log.error("Execute batch sql is empty");
            throw new Exception("Execute batch sql is empty");
        }

        if (datas!=null) {
            log.error("Execute batch data is empty");
            throw new Exception("Execute batch data is empty");
        }

        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dsHolder.getConnection(id);
            if (null != connection) {
                connection.setAutoCommit(false);
                pstmt = connection.prepareStatement(sql);
                //每1000条commit一次
                int n = 10000;

                for (Map<String, Object> map : datas) {
                    int i = 1;
                    for (ColumnField columnField : headers) {
                        Object obj = map.get(columnField.getName());
                        switch (SqlColumn.toJavaType(columnField.getType())) {
                            case "Short":
                                pstmt.setShort(i, null == obj || String.valueOf(obj).equals(Consts.EMPTY) ? (short) 0 : Short.parseShort(String.valueOf(obj).trim()));
                                break;
                            case "Integer":
                                pstmt.setInt(i, null == obj || String.valueOf(obj).equals(Consts.EMPTY) ? 0 : Integer.parseInt(String.valueOf(obj).trim()));
                                break;
                            case "Long":
                                pstmt.setLong(i, null == obj || String.valueOf(obj).equals(Consts.EMPTY) ? 0L : Long.parseLong(String.valueOf(obj).trim()));
                                break;
                            case "BigDecimal":
                                pstmt.setBigDecimal(i, null == obj || String.valueOf(obj).equals(Consts.EMPTY) ? null : (BigDecimal) obj);
                                break;
                            case "Float":
                                pstmt.setFloat(i, null == obj || String.valueOf(obj).equals(Consts.EMPTY) ? 0.0F : Float.parseFloat(String.valueOf(obj).trim()));
                                break;
                            case "Double":
                                pstmt.setDouble(i, null == obj || String.valueOf(obj).equals(Consts.EMPTY) ? 0.0D : Double.parseDouble(String.valueOf(obj).trim()));
                                break;
                            case "String":
                                pstmt.setString(i, (String) obj);
                                break;
                            case "Boolean":
                                pstmt.setBoolean(i, null != obj && Boolean.parseBoolean(String.valueOf(obj).trim()));
                                break;
                            case "Bytes":
                                pstmt.setBytes(i, (byte[]) obj);
                                break;
                            case "Date":
                                if (obj == null) {
                                    pstmt.setDate(i, null);
                                } else {
                                    java.util.Date date = (java.util.Date) obj;
                                    pstmt.setDate(i, DateUtils.toSqlDate(date));
                                }
                                break;
                            case "DateTime":
                                if (obj == null) {
                                    pstmt.setTimestamp(i, null);
                                } else {
                                    if (obj instanceof LocalDateTime) {
                                        pstmt.setTimestamp(i, Timestamp.valueOf((LocalDateTime) obj));
                                    } else {
                                        DateTime dateTime = (DateTime) obj;
                                        pstmt.setTimestamp(i, DateUtils.toTimestamp(dateTime));
                                    }
                                }
                                break;
                            case "Timestamp":
                                if (obj == null) {
                                    pstmt.setTimestamp(i, null);
                                } else {
                                    if (obj instanceof LocalDateTime) {
                                        pstmt.setTimestamp(i, Timestamp.valueOf((LocalDateTime) obj));
                                    } else {
                                        pstmt.setTimestamp(i, (Timestamp) obj);
                                    }
                                }
                                break;
                            case "Blob":
                                pstmt.setBlob(i, null == obj ? null : (Blob) obj);
                                break;
                            case "Clob":
                                pstmt.setClob(i, null == obj ? null : (Clob) obj);
                                break;
                            default:
                                pstmt.setObject(i, obj);
                        }
                        i++;
                    }

                    pstmt.addBatch();
                    if (i % n == 0) {
                        try {
                            pstmt.executeBatch();
                            connection.commit();
                        } catch (BatchUpdateException e) {
                        }
                    }
                }

                pstmt.executeBatch();
                connection.commit();
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            if (null != connection) {
                try {
                    connection.rollback();
                } catch (SQLException se) {
                    log.error(se.toString(), se);
                }
            }
            throw new Exception(e.getMessage(), e);
        } finally {
            if (null != pstmt) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    log.error(e.toString(), e);
                    throw new Exception(e.getMessage(), e);
                }
            }
            connection.close();
        }
    }

    /*
     * Execute batch sql
     */
    public Boolean executeBatch(Integer id, String sql, List<ImporterUploadReqType.ColFieldType> headers, List<CsvRow> datas) throws Exception {
        if (StringUtils.isEmpty(sql) || datas==null) {
            log.error("Execute batch parameter is empty");
            throw new Exception("Execute batch parameter is empty");
        }

        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dsHolder.getConnection(id);
            if (null != connection) {
                connection.setAutoCommit(false);
                pstmt = connection.prepareStatement(sql);
                for (int rowIdx=0; rowIdx<datas.size(); rowIdx++) {
                    // VALUES(?,?,?), it start from 1
                    int colIdx = 1;

                    CsvRow csvRow = datas.get(rowIdx);
                    for (ImporterUploadReqType.ColFieldType col : headers) {
                        Object obj = csvRow.getByName(col.alias);
                        if(obj!=null && col.javaType.equalsIgnoreCase("BOOLEAN")){
                            // Boolean.parseBoolean(null) is false
                            // don't set null here for bool
                            pstmt.setObject(colIdx, Boolean.parseBoolean((String) obj), BOOLEAN);
                        } else {
                            pstmt.setObject(colIdx, obj);
                        }
                        colIdx++;
                    }
                    pstmt.addBatch();
                }

                // commit to database
                pstmt.executeBatch();
                connection.commit();
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            if (null != connection) {
                try {
                    connection.rollback();
                } catch (SQLException se) {
                    log.error(se.toString(), se);
                }
            }
            return false;
        } finally {
            if (null != pstmt) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    log.error(e.toString(), e);
                }
            }
            connection.close();
            return true;
        }
    }

    /*
     * Execute batch sql
     */
    public void executeBatch(Integer id, String sql, Map<String, String> headers, List<CsvRow> datas) throws Exception {

        if (StringUtils.isEmpty(sql) || datas==null) {
            log.error("Execute batch parameter is empty");
            throw new Exception("Execute batch parameter is empty");
        }

        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = dsHolder.getConnection(id);
            if (null != connection) {
                connection.setAutoCommit(false);
                pstmt = connection.prepareStatement(sql);
                int n = 10000;
                String tsFormat = "";
                for (int rowIdx=0; rowIdx<datas.size(); rowIdx++) {
                    int colIdx = 1; // VALUES(?,?,?), it start from 1
                    CsvRow csvRow = datas.get(rowIdx);
                    for (String colName : headers.keySet()) {
                        Object obj = csvRow.getByName(colName);
                        if(obj == null){
                            pstmt.setObject(colIdx, obj);
                            colIdx++;
                            continue;
                        }
                        String tmp = String.valueOf(obj).trim();
                        switch (SqlColumn.toJavaType(headers.get(colName))) {
                            case "Integer":
                                //pstmt.setNull(i, INTEGER);
                                //pstmt.setInt(i,  Integer.parseInt(tmp));
                                pstmt.setObject(colIdx, obj);
                                break;
                            case "Long":
                                //pstmt.setNull(i, BIGINT);
                                //pstmt.setLong(i,  Long.parseLong(tmp));
                                pstmt.setObject(colIdx, obj);
                                break;
                            case "Float":
                                //pstmt.setNull(i, FLOAT);
                                //pstmt.setFloat(i, Float.parseFloat(tmp));
                                pstmt.setObject(colIdx, obj);
                                break;
                            case "Double":
                                //pstmt.setNull(i, DOUBLE);
                                //pstmt.setDouble(i, Double.parseDouble(tmp));
                                pstmt.setObject(colIdx, obj);
                                break;
                            case "String":
                                //pstmt.setString(i, (String) obj);
                                pstmt.setObject(colIdx, obj);
                                break;
                            case "Boolean":
                                //pstmt.setNull(i, BOOLEAN);
                                //pstmt.setBoolean(i, Boolean.parseBoolean(tmp));
                                pstmt.setObject(colIdx, obj);
                                break;
                            case "Bytes":
                                pstmt.setBytes(colIdx, (byte[]) obj);
                                break;
                            case "Date":
                            case "Time":
                            case "DateTime":
                            case "Timestamp":
                                // Date, "2023-03-01"
                                // Time, "02:00:00"
                                // Datatime, "2023-03-01 02:00:00"
                                // Timestamp, "2023-03-01 02:00:00.0"
                                //pstmt.setTimestamp(i, Timestamp.valueOf(tmp));
                                //pstmt.setString(i, tmp);
                                pstmt.setObject(colIdx, obj);
                                break;
                            case "Blob":
                                pstmt.setBlob(colIdx, null == obj ? null : (Blob) obj);
                                break;
                            case "Clob":
                                pstmt.setClob(colIdx, null == obj ? null : (Clob) obj);
                                break;
                            default:
                                pstmt.setObject(colIdx, obj);
                        }
                        colIdx++;
                    }

                    pstmt.addBatch();
                    if (rowIdx % n == 0) {
                        try {
                            pstmt.executeBatch();
                            connection.commit();
                            progress++;
                        } catch (BatchUpdateException e) {
                        }
                    }
                }

                pstmt.executeBatch();
                connection.commit();
                progress++;
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
            if (null != connection) {
                try {
                    connection.rollback();
                } catch (SQLException se) {
                    log.error(se.toString(), se);
                }
            }
            throw new Exception(e.getMessage(), e);
        } finally {
            if (null != pstmt) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    log.error(e.toString(), e);
                    throw new Exception(e.getMessage(), e);
                }
            }
            connection.close();
        }
    }

    public String formatSqlType(String type) throws Exception {
        if (!StringUtils.isEmpty(type.trim())) {
            type = type.trim().toUpperCase();
            Matcher matcher = Consts.PATTERN_DB_COLUMN_TYPE.matcher(type);
            if (!matcher.find()) {
                return SqlType.getType(type);
            } else {
                return type;
            }
        }
        return null;
    }

    public String getDbnameFromUrl(String jdbcUrl){
        int pos;
        String dbName = null;

        if(StrUtil.isEmpty(jdbcUrl)){
            return null;
        }

        jdbcUrl = jdbcUrl.toLowerCase();

        if(!jdbcUrl.startsWith("jdbc:") || (pos=jdbcUrl.indexOf(":", 5))<0){
            return null;
        }

        jdbcUrl = jdbcUrl.substring(pos+1);
        if(!jdbcUrl.startsWith("//") || (pos=jdbcUrl.indexOf("/", 3))<0){
            return null;
        }

        dbName = jdbcUrl.substring(pos+1);
        if(jdbcUrl.contains("?")){
            dbName = jdbcUrl.substring(0, jdbcUrl.indexOf("?"));
        }

        if(jdbcUrl.contains(";")){
            dbName = jdbcUrl.substring(0, jdbcUrl.indexOf("?"));
        }

        return dbName;
    }
}

