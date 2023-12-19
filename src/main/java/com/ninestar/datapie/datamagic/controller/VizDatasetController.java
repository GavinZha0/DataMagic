package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.*;
import com.ninestar.datapie.datamagic.repository.DataSourceRepository;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.repository.VizDatasetRepository;
import com.ninestar.datapie.datamagic.repository.VizDataviewRepository;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.datamagic.utils.SqlUtils;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.ColumnField;
import com.ninestar.datapie.framework.model.TreeSelect;
import com.ninestar.datapie.datamagic.utils.DbUtils;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.tablesaw.api.*;
import javax.annotation.Resource;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 *  Controller
 * </p>
 *
 * @author Gavin.Zhao
 * @since 2021-09-18
 */
@RestController
@RequestMapping("/dataset")
@Api(tags = "Dataset")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class VizDatasetController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    public DataSourceRepository sourceRepository;

    @Resource
    public VizDatasetRepository datasetRepository;

    @Resource
    public VizDataviewRepository dataviewRepository;

    @Resource
    public SysOrgRepository orgRepository;

    @Resource
    private DbUtils dbUtils;

    @PostMapping("/list")
    @ApiOperation(value = "getDatasetList", httpMethod = "POST")
    public UniformResponse getDatasetList(@RequestBody @ApiParam(name = "req", value = "request") TableListReqType req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        Long totalRecords = 0L;
        Page<VizDatasetEntity> pageEntities = null;
        List<VizDatasetEntity> queryEntities = null;

        // put multiple orders into a sort which will be put into a pageable
        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort sortable = null;
        Pageable pageable = null;

        // build sort object
        //support multiple orders
        if(req.sorter!=null && req.sorter.orders.length>0){
            for(int i=0; i< req.sorter.fields.length; i++){
                Sort.Order order = null;
                if(req.sorter.orders[i].equalsIgnoreCase("ascend")){
                    order = new Sort.Order(Sort.Direction.ASC, req.sorter.fields[i]);
                }
                else{
                    order = new Sort.Order(Sort.Direction.DESC, req.sorter.fields[i]);
                }
                orders.add(order);
            }
            sortable = Sort.by(orders);
        }

        // build page object with/without sort
        if(req.page!=null){
            // jpa page starts from 0
            if(req.sorter!=null && req.sorter.fields.length>0){
                pageable = PageRequest.of(req.page.current-1, req.page.pageSize, sortable);
            }
            else{
                pageable = PageRequest.of(req.page.current-1, req.page.pageSize);
            }
        }

        // build JPA specification
        Specification<VizDatasetEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenUsername, req.filter, req.search);

        // query data from database
        if(pageable!=null){
            pageEntities = datasetRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = datasetRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<DatasetListRspType> rspList = new ArrayList<DatasetListRspType>();
        for(VizDatasetEntity entity: queryEntities){
            if(tokenIsSuperuser || entity.getCreatedBy().equals(tokenUsername) || entity.getPubFlag()) {
                // filter by userId and pubFlag
                // this filter can be moved to specification later. Gavin!!!
                DatasetListRspType item = new DatasetListRspType();
                BeanUtil.copyProperties(entity, item, new String[]{"variable", "fields", "graph"});
                item.sourceId = entity.getDatasource().getId();
                item.sourceName = entity.getDatasource().getGroup() + "/" + entity.getDatasource().getName();
                item.variable = new JSONArray(entity.getVariable()); // convert string to json array
                item.fields = new JSONArray(entity.getField());
                item.graph = new JSONObject(entity.getGraph());

                // get related dataset count
                item.usage = dataviewRepository.countByDatasetId(entity.getId());
                rspList.add(item);
            }
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", rspList);
        jsonResponse.set("total", totalRecords);
        jsonResponse.set("current", req.page.current);

        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/tree")
    @ApiOperation(value = "getDatasetTree", httpMethod = "POST")
    public UniformResponse getDatasetTree(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        // jpa page is starting with 0
        List<VizDatasetEntity> datasetEntities = datasetRepository.findAll();

        // convert list to tree by category
        Map<String, List<VizDatasetEntity>> datasetMap = datasetEntities.stream().collect(Collectors.groupingBy(t -> t.getGroup()));
        List<TreeSelect> treeDatasets = new ArrayList<>();
        Integer i = 1000;
        for(String group : datasetMap.keySet()){
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for(VizDatasetEntity source: datasetMap.get(group)){
                if(tokenIsSuperuser || source.getOrg().getId().equals(tokenOrgId)){
                    TreeSelect treeNode = new TreeSelect(source.getId(), source.getDatasource().getType(), source.getName(), source.getName(), true, true);
                    treeGroup.getChildren().add(treeNode);
                }
            }
            if(treeGroup.getChildren().size()>0){
                treeDatasets.add(treeGroup);
            }
            i+=100;
        }

        return UniformResponse.ok().data(treeDatasets);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @ApiOperation(value = "createDataset", httpMethod = "POST")
    public UniformResponse createDataset(@RequestBody @ApiParam(name = "req", value = "dataset info") DatasetActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(req.name) || req.sourceId<1 || StrUtil.isEmpty(req.query)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<VizDatasetEntity> duplicatedEntities = datasetRepository.findByNameAndGroup(req.name, req.group);
        if(duplicatedEntities!=null){
            for(VizDatasetEntity entity: duplicatedEntities){
                if(entity.getDatasource().getId() == req.sourceId){
                    return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
                }
            }
        }

        DataSourceEntity source = sourceRepository.findById(req.sourceId).get();
        if(source==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        try {
            VizDatasetEntity newEntity = new VizDatasetEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.desc);
            newEntity.setGroup(req.group);
            if(req.variable!=null){
                newEntity.setVariable(req.variable.toString());
            }
            newEntity.setQuery(req.query);

            newEntity.setField(req.fields.toString());
            if(req.graph!=null){
                newEntity.setGraph(req.graph.toString());
            }
            newEntity.setGraphVer(req.graphVer);
            if(req.pubFlag==null){
                newEntity.setPubFlag(false);
            }
            else{
                newEntity.setPubFlag(req.pubFlag);
            }

            newEntity.setOrg(orgRepository.findById(tokenOrgId).get());

            //create_time and update_time are generated automatically by jpa

            // get and fill org
            newEntity.setDatasource(source);

            // convert query based on variable, fields
            String err = "";
            String finalQuery = convertSqlQuery(newEntity, null, err);
            newEntity.setFinalQuery(finalQuery);
            if(!StrUtil.isEmpty(err)){
                logger.error(err);
            }

            // save source
            datasetRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @ApiOperation(value = "updateDataset", httpMethod = "POST")
    public UniformResponse updateDataset(@RequestBody @ApiParam(name = "req", value = "dataset info") DatasetActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(req.id==0 || StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.query) || req.sourceId<1){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizDatasetEntity targetEntity = datasetRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        } else if(!targetEntity.getCreatedBy().equals(tokenUsername)){
            // only author can update it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            targetEntity.setId(req.id);
            targetEntity.setName(req.name);
            targetEntity.setDesc(req.desc);
            targetEntity.setGroup(req.group);
            targetEntity.setVariable(req.variable.toString());
            targetEntity.setQuery(req.query);
            targetEntity.setField(req.fields.toString());
            targetEntity.setGraph(req.graph.toString());
            targetEntity.setGraphVer(req.graphVer);

            // convert query based on variable, fields
            String err = "";
            String finalQuery = convertSqlQuery(targetEntity, null, err);
            targetEntity.setFinalQuery(finalQuery);
            if(!StrUtil.isEmpty(err)){
                logger.error(err);
            }
            //create_time and update_time are generated automatically by jpa

            // update source
            datasetRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.SHARE)
    @PostMapping("/public")
    @ApiOperation(value = "publicDataset", httpMethod = "POST")
    public UniformResponse publicDataset(@RequestBody @ApiParam(name = "params", value = "dataset id and pub flag") PublicReqType params){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(params.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizDatasetEntity targetEntity = datasetRepository.findById(params.id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        } else if(!tokenIsAdmin && !targetEntity.getCreatedBy().equals(tokenUsername)){
            // only author can do it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // update public status
            targetEntity.setPubFlag(params.pub);
            datasetRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.CLONE)
    @PostMapping("/clone")
    @ApiOperation(value = "cloneDataset", httpMethod = "POST")
    public UniformResponse cloneDataset(@RequestBody @ApiParam(name = "param", value = "dataset id") JSONObject param){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Integer id = Integer.parseInt(param.get("id").toString());
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizDatasetEntity targetEntity = datasetRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String copyName = targetEntity.getName();
        List<VizDatasetEntity> targetCopies = datasetRepository.findByNameContainingOrderByIdDesc(copyName+"(");
        if(targetCopies.size()>0){
            String tmp = targetCopies.get(0).getName();
            tmp = tmp.substring(tmp.indexOf("(")+1, tmp.indexOf(")"));
            Pattern pattern = Pattern.compile("[0-9]*");
            if(pattern.matcher(tmp).matches())
            {
                Integer idx = Integer.parseInt(tmp)+1;
                copyName += "(" + idx.toString() + ")";
            }
            else{
                copyName += "(1)";
            }
        }
        else{
            copyName += "(1)";
        }

        try {
            // update public status
            targetEntity.setId(0);
            targetEntity.setName(copyName);
            datasetRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @ApiOperation(value = "deleteDataset", httpMethod = "DELETE")
    public UniformResponse deleteDataset(@RequestParam @ApiParam(name = "id", value = "dataset id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizDatasetEntity targetEntity = datasetRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        } else if(!tokenIsAdmin && !targetEntity.getCreatedBy().equals(tokenUsername)){
            // only author can do it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        Integer usageInView = dataviewRepository.countByDatasetId(id);
        if(usageInView > 0){
            return UniformResponse.error(UniformResponseCode.DATASET_IN_USE);
        }

        try {
            // delete entity
            datasetRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/execute")
    @ApiOperation(value = "execute", httpMethod = "POST")
    public UniformResponse execute(@RequestBody @ApiParam(name = "param", value = "dataset id") DatasetExeReqType request) throws Exception {
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        DatasetResultType result = execute(request.id, request.limit);
        if(result.code == UniformResponseCode.SUCCESS){
            return UniformResponse.ok().data(result.records, result.columns, result.total);
        }
        else {
            return UniformResponse.error(result.code);
        }
    }

    @PostMapping("/get")
    @ApiOperation(value = "getDataset", httpMethod = "POST")
    public UniformResponse getDataset(@RequestBody @ApiParam(name = "param", value = "dataset id") JSONObject param){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(param.get("id").toString());

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizDatasetEntity targetEntity = datasetRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        DatasetListRspType response = new DatasetListRspType();
        BeanUtil.copyProperties(targetEntity, response, new String[]{"variable", "fields", "graph"});
        response.sourceId = targetEntity.getDatasource().getId();
        response.sourceName = targetEntity.getDatasource().getGroup() + "/" + targetEntity.getDatasource().getName();
        response.variable = new JSONArray(targetEntity.getVariable()); // convert string to json array
        response.fields = new JSONArray(targetEntity.getField());

        return UniformResponse.ok().data(response);
    }

    @PostMapping("/groups")
    @ApiOperation(value = "getGroups", httpMethod = "POST")
    public UniformResponse getGroups() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        List<Object> distinctGroups = datasetRepository.findDistinctGroup();
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", distinctGroups);
        return UniformResponse.ok().data(jsonResponse);
    }

    /*
     * execute dataset
     */
    public DatasetResultType execute(Integer datasetId, Integer limit) throws SQLException {
        DatasetResultType response = new DatasetResultType();
        if(datasetId==0){
            // dataset id is not available
            response.code = UniformResponseCode.REQUEST_INCOMPLETE;
            return response;
        }

        // find dataset first
        VizDatasetEntity datasetEntity = datasetRepository.findById(datasetId).get();
        if(datasetEntity==null || datasetEntity.getDatasource()==null){
            //dataset doesn't exist
            response.code = UniformResponseCode.DATASET_NOT_EXIST;
            return response;
        }

        // find datasource
        DataSourceEntity source = datasetEntity.getDatasource();
        if(source==null){
            // datasource doesn't exist
            response.code = UniformResponseCode.DATASOURCE_NOT_EXIST;
            return response;
        }

        // get final sql query
        String selectSqlQuery = datasetEntity.getQuery();
        String finalSqlQuery = datasetEntity.getFinalQuery();
        String err = "";
        if(selectSqlQuery.length()>10 && (finalSqlQuery==null || finalSqlQuery.length()<=0)){
            // convert to final query
            finalSqlQuery = convertSqlQuery(datasetEntity, limit, err);
        } else {
            // add limit only
            List<String> lockedTables = JSONUtil.parseArray(source.getLockedTable()).toList(String.class);
            finalSqlQuery = SqlUtils.sqlTransfer(finalSqlQuery, source.getType(), lockedTables, null, limit, false);
        }

        if(finalSqlQuery==null || finalSqlQuery.length()<=0){
            // sql doesn't exist
            response.code = UniformResponseCode.TARGET_RESOURCE_NOT_EXIST;
            return response;
        }

        if(!dbUtils.isSourceExist(source.getId())){
            // add datasource to utils
            String pwd = new String(Base64.decode(source.getPassword()));
            dbUtils.add(source.getId(), source.getName(), source.getType(), source.getUrl(), source.getParams(), source.getUsername(), pwd);
        }

        List<ColumnField> columns = new ArrayList<>();
        List<Object[]> records = new ArrayList<>();
        List<ColumnField> totalCol = new ArrayList<>();
        List<Object[]> totalRows = new ArrayList<>();
        Integer totalRowCount = null;

        try {
            dbUtils.execute(source.getId(), finalSqlQuery, columns, records);

            // get total rows of query ignoring limit
            if(source.getType().equalsIgnoreCase("MYSQL")){
                // this is working with MySql to get total records ignoring limit
                dbUtils.execute(source.getId(), "SELECT FOUND_ROWS();", totalCol, totalRows);
                if(totalRows.size()>0){
                    // total row count ignoring limit
                    totalRowCount = Integer.parseInt(totalRows.get(0)[0].toString());
                }
            }
            else {
                // how about others ???
                // general solution: two queries, select count(*) and select *
            }
        } catch (Exception e) {
            response.code = UniformResponseCode.API_EXCEPTION_SQL_EXE;
            logger.error(e.getMessage());
            return response;
        }

        JSONArray fields = new JSONArray(datasetEntity.getField());
        List<DatasetFieldType> datasetFields = JSONUtil.toList(fields,DatasetFieldType.class);
        List<ColumnField> finalColumns = new ArrayList<>();

        Table dfTable = Table.create("tmp");

        for(Integer i=0; i<columns.size(); i++){
            String colName = columns.get(i).getName();
            DatasetFieldType definedField = datasetFields.stream().filter(field -> field.name.equalsIgnoreCase(colName) || (field.alias!=null && field.alias.equalsIgnoreCase(colName))).findFirst().orElse(null);
            if(definedField==null){
                // column is not found in defined fields
                continue;
            }
            // hidden columns will not be included in result
            if(definedField.hidden==null || !definedField.hidden) {
                if(definedField.alias!=null){
                    // set name to alias which is assigned by user when dataset is created
                    // maybe it is renamed in final sql query, so do nothing here
                    columns.get(i).setName(definedField.alias);
                }

                if(definedField.metrics!=null && definedField.metrics==true){
                    // set metrics which is assigned by user when dataset is created
                    columns.get(i).setMetrics(definedField.metrics);
                }
                // build columns
                finalColumns.add(columns.get(i));

                // build data frame for agg
                Integer finalI = i;
                List<String> colData = records.stream().map(x->x[finalI]==null?null:x[finalI].toString()).collect(Collectors.toList());
                if(columns.get(i).getType().equalsIgnoreCase("NUMBER")){
                    dfTable.addColumns(DoubleColumn.create(columns.get(i).getName(), colData.stream().map(x->x==null?null:Double.parseDouble(x)).collect(Collectors.toList())));
                } else {
                    dfTable.addColumns(StringColumn.create(columns.get(i).getName(), colData));
                }
            }
        }

        List<String> columnNames = dfTable.columnNames();
        List<Object[]> finalRecords = new ArrayList<>();
        for(Row row: dfTable){
            Object[] objArray = new Object[columnNames.size()];
            for(Integer i=0; i<columnNames.size(); i++){
                objArray[i] = row.getObject(i);
            }
            finalRecords.add(objArray);
        }

        response.records = finalRecords;
        response.code = UniformResponseCode.SUCCESS;
        response.total = totalRowCount;
        response.columns = finalColumns;
        // the null field will be removed in data() by JSON set()
        return response;
    }

    public String convertSqlQuery(VizDatasetEntity datasetEntity, Integer limit, String err){
        String selectSqlQuery = datasetEntity.getQuery();
        if(selectSqlQuery.length()<=0){
            // sql doesn't exist
            err = UniformResponseCode.TARGET_RESOURCE_NOT_EXIST.getMsg();
            return null;
        }

        // find datasource
        DataSourceEntity source = datasetEntity.getDatasource();
        if(source==null){
            // datasource doesn't exist
            err = UniformResponseCode.DATASOURCE_NOT_EXIST.getMsg();
            return null;
        }

        if(StrUtil.isNotEmpty(datasetEntity.getVariable())){
            JSONArray varArray = new JSONArray(datasetEntity.getVariable());
            List<SqlUtils.VariableType> varList = JSONUtil.toList(varArray, SqlUtils.VariableType.class);
            // translate variable of sql
            selectSqlQuery = SqlUtils.sqlTranslate(selectSqlQuery, varList);
        }

        if(selectSqlQuery.length()>0 && datasetEntity.getField().length()>0){
            JSONArray fieldArray = new JSONArray(datasetEntity.getField());
            List<String> lockedTables = JSONUtil.parseArray(source.getLockedTable()).toList(String.class);
            List<SqlUtils.FieldType> fieldList = JSONUtil.toList(fieldArray, SqlUtils.FieldType.class);
            // handle dataset field config (rename, hidden, filter, sorter and limit)
            selectSqlQuery = SqlUtils.sqlTransfer(selectSqlQuery, source.getType(), lockedTables, fieldList, limit, true);
        }

        // verify selection sql
        if(selectSqlQuery.length()>0){
            UniformResponseCode validSql = SqlUtils.sqlValidate(selectSqlQuery, source.getType());
            if(validSql!=UniformResponseCode.SUCCESS){
                err = UniformResponseCode.SQL_SECURITY_RISK.getMsg();
                return null;
            }
        }

        return selectSqlQuery;
    }
}
