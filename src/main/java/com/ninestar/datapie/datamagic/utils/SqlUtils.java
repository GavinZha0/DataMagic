package com.ninestar.datapie.datamagic.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.*;
import com.alibaba.druid.sql.ast.expr.*;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import com.alibaba.druid.stat.TableStat;
import com.alibaba.druid.wall.WallCheckResult;
import com.alibaba.druid.wall.WallProvider;
import com.alibaba.druid.wall.spi.MySqlWallProvider;
import com.ninestar.datapie.datamagic.bridge.DatasetFieldType;
import com.ninestar.datapie.datamagic.bridge.ReportPageType;
import com.ninestar.datapie.framework.consts.*;
import com.ninestar.datapie.framework.model.TreeSelect;
import com.ninestar.datapie.framework.utils.UniformResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.text.StrSubstitutor;
import org.hibernate.boot.model.source.spi.TableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.*;


@Slf4j
@Component
@Scope("prototype")
public class SqlUtils {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String TABLE = "TABLE";

    private final String VIEW = "VIEW";

    private final String[] TABLE_TYPES = new String[]{TABLE, VIEW};

    public static class VariableType {
        public String name;
        public String type;
        public String value;
    }

    public static class FieldType {
        public String name;
        public String type;
        public String alias;
        public boolean hidden;
        public String filter;
        public Integer group;
        public Integer order;
    }

    public static class AttributeType {
        public HashMap<String, String> alias;
        public String[] hidden;
        public String[] filter;
        public String[] order;
    }

    /*
     * translate sql to replace variables
     * supported variable format: @variable, $variable, ${variable}
     */
    public static String sqlTranslate(String sql, JSONArray varArray) {
        String finalSql = sql;
        if((StrUtil.count(finalSql, "@")<=0 && StrUtil.count(finalSql, "$")<=0)
                || varArray==null || varArray.size()<=0){
            // no variable need to be translated
            return finalSql;
        }

        // convert var Array to List
        List<VariableType> varList = JSONUtil.toList(varArray, VariableType.class);
        finalSql = sqlTranslate(sql, varList);

        return finalSql;
    }

    /*
     * translate sql to replace variables
     * supported variable format: @variable, $variable, ${variable}
     */
    public static String sqlTranslate(String sql, List<VariableType> varList) {
        String finalSql = sql;
        if((StrUtil.count(sql, "@")<=0 && StrUtil.count(sql, "$")<=0)
                || varList==null || varList.size()==0){
            return finalSql;
        }

        Map varMap = new HashMap<>();
        for(VariableType var: varList){
            varMap.put(var.name, var.value);
            finalSql = finalSql.replaceAll("@"+var.name, var.value);
            finalSql = finalSql.replaceAll("\\$"+var.name, var.value);
        }

        // replace ${variable}
        StrSubstitutor tutor = new StrSubstitutor(varMap);
        finalSql = tutor.replace(finalSql);

        if(StrUtil.count(finalSql, "$")>0 || StrUtil.count(finalSql, "@")>0){
            // not all variables are mapped to exact value
            return "";
        } else {
            return finalSql;
        }
    }

    /*
     * translate sql to replace variables
     * supported variable format: @variable, $variable, ${variable}
     * not completed...
     */
    public static String sqlTranslate(String sql, String dbType, JSONArray varArray) {
        String finalSql = sql;
        List<VariableType> varList = new ArrayList<>();

        for(Object varItem: varArray){
            JSONObject varObj = JSONUtil.parseObj(varItem);
            VariableType var = varObj.toBean(VariableType.class);
            varList.add(var);
        }

        //parse sql string by Druid Sql Parser
        DbType type = DbType.of(dbType.toLowerCase());
        SQLStatementParser sqlParser = SQLParserUtils.createSQLStatementParser(sql, type);
        try {
            List<SQLStatement> stmtList = sqlParser.parseStatementList();
            for(SQLStatement stmt: stmtList){
                SQLSelectStatement sqlSelectStatement = (SQLSelectStatement) stmt;
                SQLWithSubqueryClause withClause = sqlSelectStatement.getSelect().getWithSubQuery();
                for(SQLWithSubqueryClause.Entry clause: withClause.getEntries()){
                    SQLSelectQuery sqlWithQuery = clause.getSubQuery().getQuery();
                    // get where and find variable like below
                }
                SQLSelectQuery sqlSelectQuery = sqlSelectStatement.getSelect().getQuery();
                if (sqlSelectQuery instanceof SQLSelectQueryBlock) {
                    // select query
                    SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) sqlSelectQuery;
                    // get where conditions
                    SQLExpr where = sqlSelectQueryBlock.getWhere();
                    List<SQLObject> conditions = where.getChildren();
                    for (SQLObject condition : conditions) {
                        // check every condition
                        if (condition instanceof SQLBinaryOpExpr) {
                            // Binary operation expression
                            SQLBinaryOpExpr sqlBinaryOpExpr = (SQLBinaryOpExpr) where;
                            SQLExpr left = sqlBinaryOpExpr.getLeft();
                            SQLBinaryOperator operator = sqlBinaryOpExpr.getOperator();
                            SQLExpr right = sqlBinaryOpExpr.getRight();
                            if(right instanceof SQLVariantRefExpr){
                                SQLVariantRefExpr sqlVar = (SQLVariantRefExpr) right;
                                System.out.println("var is detected!!! " + sqlVar.getName());
                            }
                        } else if (condition instanceof SQLInSubQueryExpr) {
                            // 如果是子查询
                            SQLInSubQueryExpr sqlInSubQueryExpr = (SQLInSubQueryExpr) condition;
                            // 处理---------------------
                        }
                    }

                }
            }
        }catch (Exception e){
            return "";
        }
        return finalSql;
    }

    /*
     * add new datasource
     */
    public static UniformResponseCode sqlValidate(String sql, String dbType) {
        //parse sql string by Druid Sql Parser
        DbType type = DbType.of(dbType.toLowerCase());
        SQLStatementParser sqlParser = SQLParserUtils.createSQLStatementParser(sql, type);
        // create visitor to get basic info of sql
        SchemaStatVisitor statVisitor = SQLUtils.createSchemaStatVisitor(type);
        try {
            List<SQLStatement> stmtList = sqlParser.parseStatementList();
            if (stmtList.size() > 1) {
                // there should be only one sql statement
                // enhance it later to allow multiple queries
                // how to use multiple sql queries ?
                return UniformResponseCode.SQL_MULTIPLE_SELECT_UNSUPPORT;
            }
            for(SQLStatement stmt: stmtList) {
                if (!(stmt instanceof SQLSelectStatement)) {
                    // only select statement is allowed
                    return UniformResponseCode.SQL_ONLY_SELECT_SUPPORT;
                } else {
                    // Preventing SQL Injection Attacks
                    WallProvider provider = new MySqlWallProvider();
                    WallCheckResult result = provider.check(sql);
                    if (!result.getViolations().isEmpty()) {
                        //injection risk !!!
                        return UniformResponseCode.SQL_SECURITY_RISK;
                    }
                }
            }
        }catch (Exception e){
            return UniformResponseCode.SQL_VALIDATION_EXCEPTION;
        }
        return UniformResponseCode.SUCCESS;
    }

    /*
     * add new datasource
     */
    public static String sqlTransfer(String sql, String dbType, List<String>lockedTables, List<FieldType> fieldList, Integer limit, Boolean cfgReplace) {
        String finalSqlStr = sql;
        List<Integer> hiddenList = new ArrayList<Integer>(); // save index of selectList for removing
        String[] groupList = new String[10]; // index is the sequence of groupBy
        String[] sorterList = new String[10]; // index is order priority

        //parse sql string by Druid Sql Parser
        DbType type = DbType.of(dbType.toLowerCase());
        SQLStatementParser sqlParser = SQLParserUtils.createSQLStatementParser(sql, type);
        try {
            List<SQLStatement> stmtList = sqlParser.parseStatementList();
            for(SQLStatement stmt: stmtList) {
                SQLSelectStatement sqlSelectStatement = (SQLSelectStatement) stmt;
                // visitor for column replacement
                SchemaStatVisitor statVisitor = SQLUtils.createSchemaStatVisitor(type);
                stmt.accept(statVisitor);

                // locked tables are not allowed to access
                if(lockedTables.size()>0){
                    Map<TableStat.Name, TableStat> tableList = statVisitor.getTables();
                    for(TableStat.Name key: tableList.keySet()){
                        for(String lockedTable: lockedTables){
                            if(key.toString().equalsIgnoreCase(lockedTable)){
                                // find locked table in query
                                System.out.println("Error: some tables are not allowed to access!");
                                return null;
                            }
                        }
                    }
                }


                SQLSelectQuery sqlSelectQuery = sqlSelectStatement.getSelect().getQuery();
                if (sqlSelectQuery instanceof SQLSelectQueryBlock) {
                    // non-union query
                    SQLSelectQueryBlock sqlSelectQueryBlock = (SQLSelectQueryBlock) sqlSelectQuery;
                    if(cfgReplace){
                        List<SQLSelectItem> selectList = sqlSelectQueryBlock.getSelectList();
                        //Collection<TableStat.Column> visitorColumns = statVisitor.getColumns();

                        // rename select columns and hide some columns based on field config
                        for (int i=0; i<selectList.size(); i++) {
                            SQLSelectItem selectItem = selectList.get(i);
                            String name = "";
                            String alias = selectItem.getAlias(); // column alias (AS xxx)

                            if (selectItem.getExpr() instanceof SQLIdentifierExpr) {
                                // no owner, no alias
                                SQLIdentifierExpr expr = (SQLIdentifierExpr) selectItem.getExpr();
                                name = expr.getName();

                            } else if (selectItem.getExpr() instanceof SQLPropertyExpr) {
                                // has owner like 'select owner.name'
                                SQLPropertyExpr expr = (SQLPropertyExpr) selectItem.getExpr();
                                //String owner = expr.getOwnerName();
                                name = expr.getName();
                            } else if (selectItem.getExpr() instanceof SQLMethodInvokeExpr) {
                                // has method like 'select count(name)'
                                SQLMethodInvokeExpr expr = (SQLMethodInvokeExpr) selectItem.getExpr();
                                name = expr.toString();
                                //String method = expr.getMethodName();
                            } else if (selectItem.getExpr() instanceof SQLAllColumnExpr) {
                                // 'select *'
                                SQLAllColumnExpr expr = (SQLAllColumnExpr) selectItem.getExpr();
                                String star = expr.toString();
                                if(star.equals("*")){
                                    // how to replace * with exact filed names ??? Gavin
                                }
                            }

                            if(!StrUtil.isEmpty(name)){
                                String finalName = name;
                                FieldType column = fieldList.stream().filter(field -> field.name.equalsIgnoreCase(finalName)).findAny().orElse(null);
                                if(column!=null){
                                    if(column.hidden){
                                        // save index into hiddenList
                                        hiddenList.add(i);
                                    }
                                    if(column.alias!=null){
                                        // rename column field
                                        selectItem.setAlias(column.alias);
                                    }
                                }
                            }
                        }

                        // delete hidden columns from query
                        if(hiddenList.size()>0){
                            for(int idx=hiddenList.size()-1; idx>=0; idx--){
                                // remove hidden column
                                int index = hiddenList.get(idx);
                                selectList.remove(index);
                            }
                        }
                    }

                    if(fieldList!=null && fieldList.size()>0) {
                        for (FieldType field : fieldList) {
                            // add filter to where
                            if (StrUtil.isNotEmpty(field.filter)) {
                                // filter doesn't have priority so add here directly
                                sqlSelectQueryBlock.addCondition(field.name + " " + field.filter);
                            }

                            // collect groupBy
                            if (field.group != null) {
                                // group has priority
                                // so collect here and add them later
                                groupList[Math.abs(field.group)] = field.name;
                            }

                            // collect sorter and priority
                            if (field.order != null) {
                                Integer priority = Math.abs(field.order);
                                String direction = " ASC";
                                if (field.order < 0) {
                                    direction = " DESC";
                                }
                                //sorter has priority
                                // so collect here and add them later
                                sorterList[priority] = field.name + direction;
                            }
                        }

                        for (String group : groupList) {
                            if (group != null) {
                                // build groupBy item based on group
                                // index is priority
                                SQLExpr groupByExpr = SQLUtils.toSQLExpr(group, type);
                                // get groupBy form sql
                                SQLSelectGroupByClause sqlGroupBy = sqlSelectQueryBlock.getGroupBy();
                                if (sqlGroupBy == null) {
                                    // new a groupBy
                                    SQLSelectGroupByClause groupByClause = new SQLSelectGroupByClause();
                                    groupByClause.addItem(groupByExpr);
                                    sqlSelectQueryBlock.setGroupBy(groupByClause);
                                } else {
                                    // add an item to groupBy
                                    sqlGroupBy.addItem(groupByExpr);
                                }
                            }
                        }

                        for (String sorter : sorterList) {
                            if (sorter != null) {
                                // build order item based on sorter
                                // index is priority
                                SQLSelectOrderByItem orderByItem = SQLUtils.toOrderByItem(sorter, type);
                                // get orderBy form sql
                                SQLOrderBy sqlOrderBy = sqlSelectQueryBlock.getOrderBy();
                                if (sqlOrderBy == null) {
                                    // new a orderBy
                                    SQLOrderBy orderBy = new SQLOrderBy();
                                    orderBy.addItem(orderByItem);
                                    sqlSelectQueryBlock.addOrderBy(orderBy);
                                } else {
                                    // add an item to orderBy
                                    sqlOrderBy.addItem(orderByItem);
                                }
                            }
                        }
                    }

                    if(limit!=null && limit>0){
                        // set or update limit
                        SQLLimit sqlLimit = sqlSelectQueryBlock.getLimit();
                        if(sqlLimit==null){
                            // set limit
                            sqlSelectQueryBlock.setLimit(new SQLLimit(limit));
                        }
                        else{
                            // there is limit statement in sql query
                            String rowCount = sqlLimit.getRowCount().toString();
                            if(limit<Integer.parseInt(rowCount)){
                                // update limit if config is less than the limit of sql
                                sqlLimit.setRowCount(limit);
                            }
                        }
                    }

                    finalSqlStr = SQLUtils.toSQLString(stmt);
                    //System.out.println("Transformed SQL:\n" + finalSqlStr);
                } else if (sqlSelectQuery instanceof SQLUnionQuery) {
                    // union的查询语句
                    // 处理---------------------
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
        return finalSqlStr;
    }

    /*
     * add new datasource
     */
    public static String sqlTransfer(String sql, String dbType, JSONArray fieldArray, Integer limit) {
        List<FieldType> fieldList = new ArrayList<>();
        if(fieldArray!=null) {
            for (Object fieldItem : fieldArray) {
                JSONObject fieldObj = JSONUtil.parseObj(fieldItem);
                FieldType field = fieldObj.toBean(FieldType.class);
                fieldList.add(field);
            }
        }

        return SqlUtils.sqlTransfer(sql, dbType, null, fieldList, limit, true);
    }
}

