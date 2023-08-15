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
import com.ninestar.datapie.datamagic.entity.DataSourceEntity;
import com.ninestar.datapie.datamagic.entity.VizDatasetEntity;
import com.ninestar.datapie.datamagic.entity.VizReportEntity;
import com.ninestar.datapie.datamagic.entity.VizViewEntity;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.repository.VizDatareportRepository;
import com.ninestar.datapie.datamagic.repository.VizDatasetRepository;
import com.ninestar.datapie.datamagic.repository.VizDataviewRepository;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.datamagic.utils.SqlUtils;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.ColumnField;
import com.ninestar.datapie.datamagic.utils.DbUtils;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.ArrayUtils;
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
import tech.tablesaw.aggregate.AggregateFunction;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.numbers.NumberColumnFormatter;

import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

/**
 * <p>
 *  Controller
 * </p>
 *
 * @author Gavin.Zhao
 * @since 2021-09-18
 */
@RestController
@RequestMapping("/dataview")
@Api(tags = "Dataview")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class VizViewController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    public VizDataviewRepository dataviewRepository;

    @Resource
    public VizDatasetRepository datasetRepository;

    @Resource
    public VizDatareportRepository reportRepository;

    @Resource
    private SysOrgRepository orgRepository;

    @Resource
    private DbUtils dbUtils;

    @PostMapping("/list")
    @ApiOperation(value = "getDataviewList", httpMethod = "POST")
    public UniformResponse getDataviewList(@RequestBody @ApiParam(name = "req", value = "request") TableListReqType req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Long totalRecords = 0L;
        Page<VizViewEntity> pageEntities = null;
        List<VizViewEntity> queryEntities = null;

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
        Specification<VizViewEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser,req.filter, req.search);

        // query data from database
        if(pageable!=null){
            pageEntities = dataviewRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = dataviewRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<DataviewListRspType> rspList = new ArrayList<DataviewListRspType>();
        for(VizViewEntity entity: queryEntities){
            if(tokenIsSuperuser || entity.getCreatedBy().equals(tokenUsername) || entity.getPubFlag()) {
                DataviewListRspType item = new DataviewListRspType();
                BeanUtil.copyProperties(entity, item, new String[]{"dim", "metrics", "filter", "sorter", "variable", "calculation", "model", "libCfg"});
                item.pubFlag = entity.getPubFlag();
                item.datasetId = entity.getDataset().getId();
                item.datasetName = entity.getDataset().getGroup() + "/" + entity.getDataset().getName();
                item.dim = JSONUtil.parseArray(entity.getDim()).toList(String.class);
                item.metrics = JSONUtil.parseArray(entity.getMetrics()).toList(String.class);
                if (entity.getRelation()!=null && StrUtil.isNotEmpty(entity.getRelation().toString())) {
                    item.relation = JSONUtil.parseArray(entity.getRelation()).toList(String.class);
                }
                if (entity.getLocation()!=null && StrUtil.isNotEmpty(entity.getLocation().toString())) {
                    item.location = JSONUtil.parseArray(entity.getLocation()).toList(String.class);
                }
                if (entity.getVariable() != null && entity.getVariable() != "") {
                    item.variable = new JSONArray(entity.getVariable()); // convert string to json array
                }
                if (entity.getCalculation() != null && entity.getCalculation() != "") {
                    item.calculation = new JSONArray(entity.getCalculation()); // convert string to json array
                }
                if (entity.getModel() != null && entity.getModel() != "") {
                    item.model = new JSONObject(entity.getModel()); // convert string to json object
                }
                if (entity.getFilter() != null && entity.getFilter() != "") {
                    item.filter = new JSONObject(entity.getFilter());
                }
                if (entity.getSorter() != null && entity.getSorter() != "") {
                    item.sorter = new JSONArray(entity.getSorter());
                }
                item.libCfg = new JSONObject(entity.getLibCfg());

                // get related view count
                List<VizReportEntity> reports = reportRepository.findAll();
                Long reportCount = reports.stream().filter(p->p.getViewIds().toString().contains(entity.getId().toString())).count();
                item.usage = reportCount.intValue();
                rspList.add(item);
            }
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", rspList);
        jsonResponse.set("total", totalRecords);
        jsonResponse.set("current", req.page.current);

        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/cat_views")
    @ApiOperation(value = "getViewsByGroup", httpMethod = "POST")
    public UniformResponse getViewsByGroup(@RequestBody @ApiParam(name = "request", value = "group") JSONObject request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Boolean tokenSuperuser = auth.getAuthorities().contains("ROLE_superuser");
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        List<VizViewEntity> queryEntities = dataviewRepository.findByGroupOrderByName(request.get("group").toString());
        Integer totalRecords = queryEntities.size();

        // build response
        List<DataviewListRspType> rspList = new ArrayList<DataviewListRspType>();
        for(VizViewEntity entity: queryEntities){
            DataviewListRspType item = new DataviewListRspType();
            BeanUtil.copyProperties(entity, item, new String[]{"dim", "metrics", "filter", "sorter", "variable", "calculation", "model", "libCfg"});
            item.pubFlag = entity.getPubFlag();
            item.datasetId = entity.getDataset().getId();
            item.datasetName = entity.getDataset().getGroup() + "/" + entity.getDataset().getName();
            item.dim = JSONUtil.parseArray(entity.getDim()).toList(String.class);
            item.metrics = JSONUtil.parseArray(entity.getMetrics()).toList(String.class);
            if (entity.getRelation() != null && entity.getRelation() != "") {
                item.relation = JSONUtil.parseArray(entity.getRelation()).toList(String.class);
            }
            if (entity.getLocation() != null && entity.getLocation() != "") {
                item.location = JSONUtil.parseArray(entity.getLocation()).toList(String.class);
            }
            if(entity.getVariable()!=null && entity.getVariable()!=""){
                item.variable = new JSONArray(entity.getVariable()); // convert string to json array
            }
            if(entity.getCalculation()!=null && entity.getCalculation()!=""){
                item.calculation = new JSONArray(entity.getCalculation()); // convert string to json array
            }
            if(entity.getModel()!=null && entity.getModel()!=""){
                item.model = new JSONObject(entity.getModel()); // convert string to json object
            }
            if(entity.getFilter()!=null && entity.getFilter()!=""){
                item.filter = new JSONObject(entity.getFilter());
            }
            if(entity.getSorter()!=null && entity.getSorter()!=""){
                item.sorter = new JSONArray(entity.getSorter());
            }
            item.libCfg = new JSONObject(entity.getLibCfg());

            rspList.add(item);
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", rspList);
        jsonResponse.set("total", totalRecords);
        jsonResponse.set("current", 1);

        return UniformResponse.ok().data(jsonResponse);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @ApiOperation(value = "createDataview", httpMethod = "POST")
    public UniformResponse createDataview(@RequestBody @ApiParam(name = "req", value = "dataview info") DataviewActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(req.name) || req.datasetId<1){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<VizViewEntity> duplicatedEntities = dataviewRepository.findByNameAndGroup(req.name, req.group);
        if(duplicatedEntities!=null){
            for(VizViewEntity entity: duplicatedEntities){
                if(entity.getDataset().getId() == req.datasetId){
                    return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
                }
            }
        }

        VizDatasetEntity dataset = datasetRepository.findById(req.datasetId).get();
        if(dataset==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        try {
            VizViewEntity newEntity = new VizViewEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.desc);
            newEntity.setGroup(req.group);
            newEntity.setType(req.type);
            newEntity.setDim(req.dim.toString());
            newEntity.setMetrics(req.metrics.toString());
            newEntity.setAgg(req.agg);
            newEntity.setPrec(req.prec);
            if(req.relation!=null){
                newEntity.setRelation(req.relation.toString());
            }
            if(req.location!=null){
                newEntity.setRelation(req.location.toString());
            }
            if(req.filter!=null){
                newEntity.setFilter(req.filter.toString());
            }
            if(req.sorter!=null){
                newEntity.setSorter(req.sorter.toString());
            }
            if(req.variable!=null){
                newEntity.setVariable(req.variable.toString());
            }
            if(req.calculation!=null){
                newEntity.setCalculation(req.calculation.toString());
            }
            if(req.model!=null){
                newEntity.setModel(req.model.toString());
            }
            newEntity.setLibName(req.libName);
            newEntity.setLibVer(req.libVer);
            newEntity.setLibCfg(req.libCfg.toString());
            newEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jpa

            newEntity.setOrg(orgRepository.findById(tokenOrgId).get());

            // get and fill org
            newEntity.setDataset(dataset);

            // save source
            dataviewRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @ApiOperation(value = "updateDataview", httpMethod = "POST")
    public UniformResponse updateDataview(@RequestBody @ApiParam(name = "req", value = "dataview info") DataviewActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(req.id==0 || StrUtil.isEmpty(req.name) || req.datasetId<1){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizViewEntity targetEntity = dataviewRepository.findById(req.id).get();
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
            targetEntity.setType(req.type);
            targetEntity.setDim(req.dim.toString());
            targetEntity.setMetrics(req.metrics.toString());
            targetEntity.setAgg(req.agg);
            targetEntity.setPrec(req.prec);
            if(req.filter!=null){
                targetEntity.setFilter(req.filter.toString());
            }
            if(req.sorter!=null){
                targetEntity.setSorter(req.sorter.toString());
            }
            if(req.variable!=null){
                targetEntity.setVariable(req.variable.toString());
            }
            if(req.calculation!=null){
                targetEntity.setCalculation(req.calculation.toString());
            }
            if(req.model!=null){
                targetEntity.setModel(req.model.toString());
            }
            targetEntity.setLibName(req.libName);
            targetEntity.setLibVer(req.libVer);
            targetEntity.setLibCfg(req.libCfg.toString());
            targetEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jpa

            // update source
            dataviewRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.SHARE)
    @PostMapping("/public")
    @ApiOperation(value = "publicDataview", httpMethod = "POST")
    public UniformResponse publicDataview(@RequestBody @ApiParam(name = "request", value = "dataview id and pub flag") PublicReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(request.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizViewEntity targetEntity = dataviewRepository.findById(request.id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        } else if(!tokenIsAdmin && !targetEntity.getCreatedBy().equals(tokenUsername)){
            // only author can do it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // update public status
            targetEntity.setPubFlag(request.pub);
            dataviewRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.GRANT)
    @PostMapping("/clone")
    @ApiOperation(value = "cloneDataview", httpMethod = "POST")
    public UniformResponse cloneDataview(@RequestBody @ApiParam(name = "request", value = "dataview id") JSONObject request){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(request.get("id").toString());
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizViewEntity targetEntity = dataviewRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String copyName = targetEntity.getName();
        List<VizViewEntity> targetCopies = dataviewRepository.findByNameContainingOrderByIdDesc(copyName+"(");
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
            dataviewRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @ApiOperation(value = "deleteDataview", httpMethod = "DELETE")
    public UniformResponse deleteDataview(@RequestParam @ApiParam(name = "id", value = "dataview id") Integer id){
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

        VizViewEntity targetEntity = dataviewRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        } else if(!tokenIsAdmin && !targetEntity.getCreatedBy().equals(tokenUsername)){
            // only author can do it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // delete entity
            dataviewRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/groups")
    @ApiOperation(value = "getGroups", httpMethod = "POST")
    public UniformResponse getGroups() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        List<Object> distinctGroups = dataviewRepository.findDistinctGroup();

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", distinctGroups);
        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/execute1")
    @ApiOperation(value = "execute1", httpMethod = "POST")
    public UniformResponse execute1(@RequestBody @ApiParam(name = "param", value = "dataset id and limit") DataviewExeReqType request) throws Exception {
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(request.id==0){
            // dataset id is not available
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        // find dataset first
        VizDatasetEntity datasetEntity = datasetRepository.findById(request.id).get();
        if(datasetEntity==null || datasetEntity.getDatasource()==null){
            //dataset doesn't exist
            return UniformResponse.error(UniformResponseCode.DATASET_NOT_EXIST);
        }

        String selectSqlQuery = datasetEntity.getQuery();
        if(selectSqlQuery.length()<=0){
            // sql doesn't exist
            return UniformResponse.error(UniformResponseCode.DATASET_NOT_EXIST);
        }

        // find datasource
        DataSourceEntity source = datasetEntity.getDatasource();
        if(source==null){
            // datasource doesn't exist
            return UniformResponse.error(UniformResponseCode.DATASOURCE_NOT_EXIST);
        }

        if(datasetEntity.getVariable().length()>0){
            JSONArray varArray = new JSONArray(datasetEntity.getVariable());
            List<SqlUtils.VariableType> varList = JSONUtil.toList(varArray, SqlUtils.VariableType.class);
            // translate variable of sql
            selectSqlQuery = SqlUtils.sqlTranslate(selectSqlQuery, varList);
        }

        if(datasetEntity.getField().length()>0){
            JSONArray fieldArray = new JSONArray(datasetEntity.getField());
            List<SqlUtils.FieldType> fieldList = JSONUtil.toList(fieldArray, SqlUtils.FieldType.class);
            List<String> lockedTables = JSONUtil.parseArray(source.getLockedTable()).toList(String.class);
            // handle dataset field config (rename, hidden, filter, sorter and limit)
            selectSqlQuery = SqlUtils.sqlTransfer(selectSqlQuery, source.getType(), lockedTables, fieldList, request.limit, true);
        }

        // verify selection sql
        UniformResponseCode validSql = SqlUtils.sqlValidate(selectSqlQuery, source.getType());
        if(validSql!=UniformResponseCode.SUCCESS){
            return UniformResponse.error(UniformResponseCode.SQL_SECURITY_RISK);
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
            dbUtils.execute(source.getId(), selectSqlQuery, columns, records);
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
            logger.error(e.getMessage());
            return UniformResponse.error(UniformResponseCode.API_EXCEPTION_SQL_EXE);
        }

        JSONArray fields = new JSONArray(datasetEntity.getField());
        List<DatasetFieldType> datasetFields = JSONUtil.toList(fields,DatasetFieldType.class);

        // update column definition
        List<Integer> hiddenCols = new ArrayList<Integer>();
        for(Integer i=columns.size()-1; i>=0; i--){
            for(DatasetFieldType item: datasetFields){
                if(item.name.equalsIgnoreCase(columns.get(i).getName())){
                    if(item.hidden!=null && item.hidden){
                        // remove hidden column
                        columns.remove(columns.get(i));
                        hiddenCols.add(i);
                    }
                    else {
                        if(item.alias!=null){
                            // set name to alias which is assigned by user when dataset is created
                            columns.get(i).setName(item.alias);
                        }
                        if(item.metrics!=null && item.metrics==true){
                            // set metrics which is assigned by user when dataset is created
                            columns.get(i).setMetrics(item.metrics);
                        }
                    }
                    break;
                }
            }
        }

        // update records to remove hidden fields
        if(hiddenCols.size()>0){
            // sort hidden columns by DESC for removing
            Collections.sort(hiddenCols, Collections.reverseOrder());
            // update records to remove all hidden fields
            List<Object[]> newRecords = new ArrayList<Object[]>();
            for(int row = 0; row < records.size(); row++){
                Object[] objs = records.get(row);
                for(int m=0; m<hiddenCols.size(); m++){
                    objs = ArrayUtils.removeElement(objs, objs[hiddenCols.get(m)]);
                }
                newRecords.add(objs);
            }
            // the null field will be removed in data() by JSON set()
            return UniformResponse.ok().data(newRecords, columns, totalRowCount);
        }
        else{
            // the null field will be removed in data() by JSON set()
            return UniformResponse.ok().data(records, columns, totalRowCount);
        }
    }

    @PostMapping("/execute")
    @ApiOperation(value = "execute", httpMethod = "POST")
    public UniformResponse execute(@RequestBody @ApiParam(name = "request", value = "id") JSONObject request) throws Exception {
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(request.get("id").toString());

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        Optional<VizViewEntity> viewEntities = dataviewRepository.findById(id);
        if(!viewEntities.isPresent()){
            return UniformResponse.error(UniformResponseCode.DATAVIEW_NOT_EXIST);
        }

        VizViewEntity viewEntity = viewEntities.get();
        DataviewLoadRspType response = new DataviewLoadRspType();
        // view id
        response.id = viewEntity.getId();
        // view config
        response.name = viewEntity.getName();
        response.group = viewEntity.getGroup();
        response.type = viewEntity.getType();
        response.libName = viewEntity.getLibName();
        response.libVer = viewEntity.getLibVer();
        response.libCfg = new JSONObject(viewEntity.getLibCfg());

        // get view data
        DatasetResultType result = execute(id);
        if(result.code == UniformResponseCode.SUCCESS){
            response.records = result.records;
            for(ColumnField col: result.columns){
                DataviewLoadRspType.Column item = new DataviewLoadRspType.Column();
                item.name = col.getName();
                item.type = col.getType();
                response.columns.add(item);
            }
            return UniformResponse.ok().data(response);
        }
        else {
            return UniformResponse.error(result.code);
        }
    }

    /*
     * execute dataset
     */
    public DatasetResultType execute(Integer dataviewId) throws IOException {
        DatasetResultType response = new DatasetResultType();

        VizViewEntity viewEntity = dataviewRepository.findById(dataviewId).get();
        if(viewEntity==null){
            response.code = UniformResponseCode.DATAVIEW_NOT_EXIST;
            return response;
        }

        VizDatasetEntity datasetEntity = viewEntity.getDataset();
        // find dataset first
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
            finalSqlQuery = convertSqlQuery(datasetEntity, null, err);
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

        try {
            dbUtils.execute(source.getId(), finalSqlQuery, columns, records);
        } catch (Exception e) {
            response.code = UniformResponseCode.API_EXCEPTION_SQL_EXE;
            logger.error(e.getMessage());
            return response;
        }

        JSONArray fields = new JSONArray(datasetEntity.getField());
        List<DatasetFieldType> datasetFields = JSONUtil.toList(fields,DatasetFieldType.class);
        List<ColumnField> queryColumns = new ArrayList<>();
        Table dfTable = Table.create("table"+dataviewId);

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
                queryColumns.add(columns.get(i));

                // build data frame for agg
                Integer finalI = i;
                List<String> colData = records.stream().map(x->x[finalI].toString()).collect(Collectors.toList());
                if(columns.get(i).getType().equalsIgnoreCase("NUMBER")){
                    dfTable.addColumns(DoubleColumn.create(columns.get(i).getName(), colData.stream().map(x->Double.parseDouble(x)).collect(Collectors.toList())));
                } else {
                    dfTable.addColumns(StringColumn.create(columns.get(i).getName(), colData));
                }
            }
        }

        // filter original data before aggregation
        String viewFilter = viewEntity.getFilter();
        if(!StrUtil.isEmpty(viewFilter)){
            JSONObject filter = new JSONObject(viewFilter);
            if(filter.size()>0){
                for(Map.Entry<String,Object> entry : filter.entrySet()){
                    if(entry.getValue().toString().startsWith(">=")){
                        Float value = Float.parseFloat(entry.getValue().toString().substring(2,10));
                        dfTable = dfTable.where(t->t.doubleColumn(entry.getKey()).isGreaterThanOrEqualTo(value));
                    } else if(entry.getValue().toString().startsWith("<=")){
                        Float value = Float.parseFloat(entry.getValue().toString().substring(2,10));
                        dfTable = dfTable.where(t->t.doubleColumn(entry.getKey()).isLessThanOrEqualTo(value));
                    } else if(entry.getValue().toString().startsWith("==")){
                        Float value = Float.parseFloat(entry.getValue().toString().substring(2,10));
                        dfTable = dfTable.where(t->t.doubleColumn(entry.getKey()).isEqualTo(value));
                    } else if(entry.getValue().toString().startsWith("!=")){
                        Float value = Float.parseFloat(entry.getValue().toString().substring(2,10));
                        dfTable = dfTable.where(t->t.doubleColumn(entry.getKey()).isNotEqualTo(value));
                    } else if(entry.getValue().toString().startsWith(">")){
                        Float value = Float.parseFloat(entry.getValue().toString().substring(1,10));
                        dfTable = dfTable.where(t->t.doubleColumn(entry.getKey()).isGreaterThan(value));
                    } else if(entry.getValue().toString().startsWith("<")){
                        Float value = Float.parseFloat(entry.getValue().toString().substring(1,10));
                        dfTable = dfTable.where(t->t.doubleColumn(entry.getKey()).isLessThan(value));
                    } else {
                        logger.error("Unsupported filter: " + entry.getKey() + entry.getValue());
                        response.code = UniformResponseCode.SQL_AGGREGATION_EXCEPTION;
                        return response;
                    }
                }
            }
        }

        // aggregation
        List<String> dim = JSONUtil.parseArray(viewEntity.getDim()).toList(String.class);
        List<String> metrics = JSONUtil.parseArray(viewEntity.getMetrics()).toList(String.class);
        String agg = viewEntity.getAgg();
        String aggColName = agg;
        if(!StrUtil.isEmpty(agg)){
            try {
                AggregateFunction aggFunc = count;
                switch (agg.toUpperCase()) {
                    case "COUNT": {
                        aggFunc = count;
                        aggColName = "Count ";
                        break;
                    }
                    case "SUM":{
                        aggFunc = sum;
                        aggColName = "Sum ";
                        break;
                    }
                    case "MEAN":{
                        aggFunc = mean;
                        aggColName = "Mean ";
                        break;
                    }
                    case "MEDIAN":{
                        aggFunc = median;
                        aggColName = "Median ";
                        break;
                    }
                    case "MIN":{
                        aggFunc = min;
                        aggColName = "Min ";
                        break;
                    }
                    case "MAX":{
                        aggFunc = max;
                        aggColName = "Max ";
                        break;
                    }
                }

                switch (metrics.size()){
                    case 1:{
                        if (dim.size() == 1){
                            dfTable = dfTable.summarize(metrics.get(0), aggFunc).by(dim.get(0));
                        } else if (dim.size() == 2){
                            dfTable = dfTable.summarize(metrics.get(0), aggFunc).by(dim.get(0), dim.get(1));
                        } else if (dim.size() == 3){
                            dfTable = dfTable.summarize(metrics.get(0), aggFunc).by(dim.get(0), dim.get(1), dim.get(2));
                        }
                        // keep original name
                        dfTable.column(aggColName + "["+metrics.get(0)+"]").setName(metrics.get(0));
                        // set precision for print format (value is not changed)
                        dfTable.doubleColumn(metrics.get(0)).setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(2));
                        break;
                    }
                    case 2:{
                        if (dim.size() == 1){
                            dfTable = dfTable.summarize(metrics.get(0), metrics.get(1), aggFunc).by(dim.get(0));
                        } else if (dim.size() == 2){
                            dfTable = dfTable.summarize(metrics.get(0), metrics.get(1), aggFunc).by(dim.get(0), dim.get(1));
                        } else if (dim.size() == 3){
                            dfTable = dfTable.summarize(metrics.get(0), metrics.get(1), aggFunc).by(dim.get(0), dim.get(1), dim.get(2));
                        }
                        // keep original name
                        dfTable.column(aggColName + "["+metrics.get(0)+"]").setName(metrics.get(0));
                        dfTable.column(aggColName + "["+metrics.get(1)+"]").setName(metrics.get(1));
                        // set precision
                        dfTable.floatColumn(metrics.get(0)).setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(2));
                        dfTable.floatColumn(metrics.get(1)).setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(2));
                        break;
                    }
                    case 3:{
                        if (dim.size() == 1){
                            dfTable = dfTable.summarize(metrics.get(0), metrics.get(1), metrics.get(2), aggFunc).by(dim.get(0));
                        } else if (dim.size() == 2){
                            dfTable = dfTable.summarize(metrics.get(0), metrics.get(1), metrics.get(2), aggFunc).by(dim.get(0), dim.get(1));
                        } else if (dim.size() == 3){
                            dfTable = dfTable.summarize(metrics.get(0), metrics.get(1), metrics.get(2), aggFunc).by(dim.get(0), dim.get(1), dim.get(2));
                        }
                        // keep original name
                        dfTable.column(aggColName + "["+metrics.get(0)+"]").setName(metrics.get(0));
                        dfTable.column(aggColName + "["+metrics.get(1)+"]").setName(metrics.get(1));
                        dfTable.column(aggColName + "["+metrics.get(2)+"]").setName(metrics.get(2));
                        // set precision
                        dfTable.floatColumn(metrics.get(0)).setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(2));
                        dfTable.floatColumn(metrics.get(1)).setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(2));
                        dfTable.floatColumn(metrics.get(2)).setPrintFormatter(NumberColumnFormatter.fixedWithGrouping(2));
                        break;
                    }
                }

            } catch (Exception e){
                logger.error(e.getMessage());
                response.code = UniformResponseCode.SQL_AGGREGATION_EXCEPTION;
                return response;
            }
        }

        // sort data after aggregation
        String viewSorter = viewEntity.getSorter();
        if(!StrUtil.isEmpty(viewSorter)){
            JSONArray sorter = new JSONArray(viewSorter);
            List<String> sortFields = new ArrayList<>();
            for(Object st: sorter){
                String[] sortExp = st.toString().split(" ");
                if(sortExp[1].equalsIgnoreCase("ASC")){
                    // ascent
                    sortFields.add(sortExp[0]);
                } else {
                    // descent
                    sortFields.add("-"+sortExp[0]);
                }
            }

            switch (sortFields.size()){
                case 1: {
                    dfTable.sortOn(sortFields.get(0));
                    break;
                }
                case 2: {
                    dfTable.sortOn(sortFields.get(0), sortFields.get(1));
                    break;
                }
                case 3: {
                    dfTable.sortOn(sortFields.get(0), sortFields.get(1), sortFields.get(2));
                    break;
                }
            }
        }

        //get final records after filter/aggregation/sort
        List<String> columnNames = dfTable.columnNames();
        List<Object[]> finalRecords = new ArrayList<>();
        for(Row row: dfTable){
            Object[] objArray = new Object[columnNames.size()];
            for(Integer i=0; i<columnNames.size(); i++){
                String javaType = row.getColumnType(i).toString();
                switch (javaType){
                    case "DOUBLE":
                    case "FLOAT":
                    case "BIGDECIMAL":
                    {
                        // set precision
                        BigDecimal tmp = new BigDecimal(row.getDouble(i));
                        objArray[i] = tmp.setScale(2, RoundingMode.HALF_UP).doubleValue();
                        break;
                    }
                    case "INTEGER":
                    case "SHORT":
                    case "LONG":
                    {
                        objArray[i] = row.getInt(i);
                        break;
                    }
                    default:{
                        objArray[i] = row.getObject(i);
                    }
                }
            }
            finalRecords.add(objArray);
        }

        // get final columns after aggregation
        List<ColumnField> finalColumns = new ArrayList<>();
        for(String colName: columnNames){
            ColumnField colField = queryColumns.stream().filter(col -> col.getName().equalsIgnoreCase(colName)).findAny().orElse(null);
            if(colField!=null){
                // the type of aggregated column should be updated to Integer/float/double
                finalColumns.add(colField);
            }
        }

        response.records = finalRecords;
        response.code = UniformResponseCode.SUCCESS;
        response.total = finalRecords.size();
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

        if(datasetEntity.getVariable().length()>0){
            JSONArray varArray = new JSONArray(datasetEntity.getVariable());
            List<SqlUtils.VariableType> varList = JSONUtil.toList(varArray, SqlUtils.VariableType.class);
            // translate variable of sql
            selectSqlQuery = SqlUtils.sqlTranslate(selectSqlQuery, varList);
        }

        if(selectSqlQuery.length()>0 && datasetEntity.getField().length()>0){
            JSONArray fieldArray = new JSONArray(datasetEntity.getField());
            List<SqlUtils.FieldType> fieldList = JSONUtil.toList(fieldArray, SqlUtils.FieldType.class);
            List<String> lockedTables = JSONUtil.parseArray(source.getLockedTable()).toList(String.class);
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
