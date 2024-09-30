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
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.repository.DataSourceRepository;
import com.ninestar.datapie.datamagic.repository.VizDatasetRepository;
import com.ninestar.datapie.datamagic.utils.DbUtils;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.datamagic.utils.SqlUtils;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.*;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.*;
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
@RequestMapping("/src/datasource")
@Tag(name = "Datasource")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class DataSourceController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    public DataSourceRepository sourceRepository;

    @Resource
    public VizDatasetRepository datasetRepository;

    @Resource
    private SysOrgRepository orgRepository;

    @Resource
    private DbUtils dbUtils;

    @PostMapping("/list")
    @Operation(description = "getSourceList")
    public UniformResponse getSourceList(@RequestBody @Parameter(name = "req", description = "request") TableListReqType req) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        Long totalRecords = 0L;
        Page<DataSourceEntity> pageEntities = null;
        List<DataSourceEntity> queryEntities = null;

        // put multiple orders into a sort which will be put into a pageable
        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort sortable = null;
        Pageable pageable = null;

        // build sort object
        //support multiple orders
        if (req.sorter != null && req.sorter.orders.length > 0) {
            for (int i = 0; i < req.sorter.fields.length; i++) {
                Sort.Order order = null;
                if (req.sorter.orders[i].equalsIgnoreCase("ascend")) {
                    order = new Sort.Order(Sort.Direction.ASC, req.sorter.fields[i]);
                } else {
                    order = new Sort.Order(Sort.Direction.DESC, req.sorter.fields[i]);
                }
                orders.add(order);
            }
            sortable = Sort.by(orders);
        }

        // build page object with/without sort
        if (req.page != null) {
            // jpa page starts from 0
            if (req.sorter != null && req.sorter.fields.length > 0) {
                pageable = PageRequest.of(req.page.current - 1, req.page.pageSize, sortable);
            } else {
                pageable = PageRequest.of(req.page.current - 1, req.page.pageSize);
            }
        }

        // build JPA specification
        Specification<DataSourceEntity> specification = JpaSpecUtil.build(tokenOrgId, tokenIsSuperuser, tokenUsername, req.filter, req.search);

        // query data from database
        if (pageable != null) {
            pageEntities = sourceRepository.findAll(specification, pageable);

            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        } else {
            queryEntities = sourceRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<SourceListRspType> rspList = new ArrayList<SourceListRspType>();
        for (DataSourceEntity entity : queryEntities) {
            if(tokenIsSuperuser || entity.getCreatedBy().equals(tokenUsername) || (entity.getOrg().getId() == tokenOrgId && entity.getPubFlag())){
                // filter by userId and pubFlag
                // this filter can be moved to specification later. Gavin!!!
                SourceListRspType item = new SourceListRspType();
                BeanUtil.copyProperties(entity, item, new String[]{"params"});
                item.params = new JSONArray(entity.getParams());
                item.pubFlag = entity.getPubFlag();
                item.password = "******"; // hide password

                // get related dataset count
                item.usage = datasetRepository.countBySourceId(entity.getId());
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
    @Operation(description = "getSourceTree")
    public UniformResponse getSourceTree() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        List<DataSourceEntity> dataSources = null;
        if(tokenIsSuperuser){
            dataSources = sourceRepository.findAll();
        } else {
            dataSources = sourceRepository.findByOrgId(tokenOrgId);
        }

        // convert list to tree by category
        Map<String, List<DataSourceEntity>> sourceMap = dataSources.stream().collect(Collectors.groupingBy(t -> t.getGroup()));
        List<TreeSelect> treeSources = new ArrayList<>();
        Integer i = 1000;
        for (String group : sourceMap.keySet()) {
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for (DataSourceEntity source : sourceMap.get(group)) {
                // selectable and isLeaf should be handled by front end. so it should be removed here
                TreeSelect treeNode = new TreeSelect(source.getId(), source.getType(), source.getName(), source.getName(), true, true);
                treeGroup.getChildren().add(treeNode);
            }
            treeSources.add(treeGroup);
            i += 100;
        }

        return UniformResponse.ok().data(treeSources);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @Operation(description = "createSource")
    public UniformResponse createSource(@RequestBody @Parameter(name = "source", description = "source info") SourceActionReqType source) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if (StrUtil.isEmpty(source.name) || StrUtil.isEmpty(source.url) || StrUtil.isEmpty(source.username) || StrUtil.isEmpty(source.password)) {
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        DataSourceEntity duplicatedSource = sourceRepository.findByName(source.name);
        if (duplicatedSource != null) {
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        try {
            DataSourceEntity newSource = new DataSourceEntity();
            //don't set ID for create
            newSource.setName(source.name);
            newSource.setDesc(source.desc);
            newSource.setType(source.type);
            if(source.group==null){
                newSource.setGroup("default");
            }
            else{
                newSource.setGroup(source.group);
            }
            newSource.setUrl(source.url);
            if(source.params!=null && source.params.size()>0){
                newSource.setParams(source.params.toString());
            }
            newSource.setUsername(source.username);
            // password is encoded by frontend (Base64.encode)
            newSource.setPassword(source.password);
            newSource.setPubFlag(false);
            if(source.version!=null){
                newSource.setVersion(source.version); // don't ask user to set it
            }
            //create_time and update_time are generated automatically by jpa

            // get and fill org
            newSource.setOrg(orgRepository.findById(tokenOrgId).get());

            // save source
            sourceRepository.save(newSource);
            return UniformResponse.ok();
        } catch (Exception e) {
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @Operation(description = "updateSource")
    public UniformResponse updateSource(@RequestBody @Parameter(name = "source", description = "source info") SourceActionReqType source) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if (source.id == 0 || StrUtil.isEmpty(source.name) || StrUtil.isEmpty(source.password)) {
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        DataSourceEntity targetEntity = sourceRepository.findById(source.id).get();
        if (targetEntity == null) {
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        } else if(!targetEntity.getCreatedBy().equals(tokenUsername)){
            // only author can update it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            if (!Base64.decode(source.password).toString().equals("******")) {
                // source password is encoded
                targetEntity.setPassword(source.password);
            }
            targetEntity.setName(source.name);
            targetEntity.setDesc(source.desc);
            targetEntity.setType(source.type);
            if(source.group==null){
                targetEntity.setGroup("default");
            }
            else{
                targetEntity.setGroup(source.group);
            }

            targetEntity.setUrl(source.url);
            targetEntity.setParams(source.params.toString());
            targetEntity.setUsername(source.username);
            targetEntity.setPassword(source.password); // password is encoded by frontend (btoa)
            targetEntity.setVersion(source.version);
            targetEntity.setPubFlag(source.pubFlag);
            //create_time and update_time are generated automatically by jpa

            // update source
            sourceRepository.save(targetEntity);
            return UniformResponse.ok();
        } catch (Exception e) {
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.CLONE)
    @PostMapping("/clone")
    @Operation(description = "cloneDatasource")
    public UniformResponse cloneDatasource(@RequestBody @Parameter(name = "param", description = "datasource id") cn.hutool.json.JSONObject param) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        Integer sourceId = Integer.parseInt(param.get("id").toString());
        if (sourceId == 0) {
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        DataSourceEntity targetEntity = sourceRepository.findById(sourceId).get();
        if (targetEntity == null) {
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
            //targetEntity.getOrg().getId().equals(tokenOrgId) - temp disable to avoid exception - Gavin
        } else if(!targetEntity.getCreatedBy().equals(tokenUsername)){
            // only the user which is in same org has permission to clone it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        String copyName = targetEntity.getName();
        List<DataSourceEntity> targetCopies = sourceRepository.findByNameContainingOrderByIdDesc(copyName + "(");
        if (targetCopies.size() > 0) {
            String tmp = targetCopies.get(0).getName();
            tmp = tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")"));
            Pattern pattern = Pattern.compile("[0-9]*");
            if (pattern.matcher(tmp).matches()) {
                Integer idx = Integer.parseInt(tmp) + 1;
                copyName += "(" + idx.toString() + ")";
            } else {
                copyName += "(1)";
            }
        } else {
            copyName += "(1)";
        }

        try {
            // update public status
            targetEntity.setId(0);
            targetEntity.setName(copyName);
            targetEntity.setPassword(""); // remove password
            sourceRepository.save(targetEntity);
            return UniformResponse.ok();
        } catch (Exception e) {
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.SHARE)
    @PostMapping("/public")
    @Operation(description = "publicSource")
    public UniformResponse publicSource(@RequestBody @Parameter(name = "params", description = "source id and pub flag") PublicReqType request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if (request.id == 0) {
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        DataSourceEntity targetSource = sourceRepository.findById(request.id).get();
        if (targetSource == null) {
            //target user doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        } else if(!tokenIsAdmin && !targetSource.getCreatedBy().equals(tokenUsername)){
            // only author can do it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // update public status
            targetSource.setPubFlag(request.pub);
            sourceRepository.save(targetSource);
            return UniformResponse.ok();
        } catch (Exception e) {
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.GRANT)
    @PostMapping("/lock")
    @Operation(description = "lockTables")
    public UniformResponse lockTables(@RequestBody @Parameter(name = "params", description = "source id and tables") LockTableReqType request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if (request.id == 0) {
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        DataSourceEntity targetSource = sourceRepository.findById(request.id).get();
        if (targetSource == null) {
            //target user doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        } else if(!tokenIsAdmin && !targetSource.getCreatedBy().equals(tokenUsername)){
            // only author can do it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // update public status
            String tables = Arrays.toString(request.names);
            targetSource.setLocked(tables);
            sourceRepository.save(targetSource);
            return UniformResponse.ok();
        } catch (Exception e) {
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @Operation(description = "deleteSource")
    public UniformResponse deleteSource(@RequestParam @Parameter(name = "id", description = "source id") Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if (id == 0) {
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        DataSourceEntity targetEntity = sourceRepository.findById(id).get();
        if (targetEntity == null) {
            //target entity doesn't exist
            return UniformResponse.ok();
        } else if(!targetEntity.getCreatedBy().equals(tokenUsername) && !tokenIsAdmin){
            // only author can do it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        Integer usageInDataset = datasetRepository.countBySourceId(id);
        if(usageInDataset > 0){
            return UniformResponse.error(UniformResponseCode.DATASOURCE_IN_USE);
        }

        try {
            // delete entity
            sourceRepository.deleteById(id);
            return UniformResponse.ok();
        } catch (Exception e) {
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/groups")
    @Operation(description = "getGroups")
    public UniformResponse getGroups() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        List<Object> distinctGroups = sourceRepository.findDistinctGroup(tokenOrgId);
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", distinctGroups);
        return UniformResponse.ok().data(jsonResponse);
    }


    @PostMapping("/test")
    @Operation(description = "testSource")
    public UniformResponse testSource(@RequestBody @Parameter(name = "request", description = "source info") SourceActionReqType request) throws Exception {
        if (StrUtil.isEmpty(request.type) || StrUtil.isEmpty(request.url) || StrUtil.isEmpty(request.username) || StrUtil.isEmpty(request.password)) {
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        DataSourceEntity targetEntity = null;
        if (request.id != null && request.id != 0) {
            targetEntity = sourceRepository.findById(request.id).get();
        }

        Boolean result = false;
        String dbVersion = null;
        try {
            if (targetEntity != null) {
                dbVersion = testSource(request.id, request.password);// via Hikari
            } else {
                dbVersion = testSource(request.type, request.url, request.params, request.username, request.password);// via Hikari
            }
        }catch (Exception e){
            return UniformResponse.error(e.getMessage());
        }

        if (dbVersion!=null) {
            return UniformResponse.ok().data(dbVersion);
        } else {
            return UniformResponse.error();
        }
    }

    @PostMapping("/databases")
    @Operation(description = "getSourceDbs")
    public UniformResponse getSourceDbs(@RequestBody @Parameter(name = "param", description = "source id") JSONObject param) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        Integer sourceId = Integer.parseInt(param.get("id").toString());
        List<String> databases = getSourceDbs(sourceId);// via Hikari

        Integer i = 0;
        List<OptionsRspType> dbList = new ArrayList<OptionsRspType>();
        for (String database : databases) {
            OptionsRspType db = new OptionsRspType();
            db.name = database;
            db.id = i;
            dbList.add(db);
            i++;
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", dbList);
        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/sets")
    @Operation(description = "getSourceSets")
    public UniformResponse getSourceSets(@RequestBody @Parameter(name = "param", description = "source id") JSONObject param) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        // sets is tables for database
        Integer sourceId = Integer.parseInt(param.get("id").toString());
        Boolean includeLocked = false;
        if(param.containsKey("locked")){
            includeLocked = Boolean.parseBoolean(param.get("locked").toString());
        }

        DataSourceEntity targetSource = sourceRepository.findById(sourceId).get();
        if(!targetSource.getOrg().getId().equals(tokenOrgId) && !tokenIsSuperuser){
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        List<String> lockedTables = JSONUtil.parseArray(targetSource.getLocked()).toList(String.class);

        // Database name is in jdbc url of targetSource so leave it null here
        DbTables dbTables = getSourceTables(sourceId, null);// via Hikari
        dbTables.setDbType(targetSource.getType());
        List<TableField> tables = dbTables.getTables();
        for (int i = tables.size() - 1; i >= 0; i--) {
            TableField field = tables.get(i);
            if (lockedTables.contains(field.getName())) {
                if(!includeLocked){
                    // remove locked table from list
                    tables.remove(field);
                }
                else{
                    field.setLocked(true);
                }
            }
        }

        return UniformResponse.ok().data(dbTables);
    }

    @PostMapping("/fields")
    @Operation(description = "getSourceFields")
    public UniformResponse getSourceFields(@RequestBody @Parameter(name = "params", description = "source id and table name") FieldReqType params) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        // Database name is in url so leave it null here
        TableColumns tableColumns = getSourceColumns(params.id, null, params.name);// via Hikari

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", tableColumns.getColumns());
        return UniformResponse.ok().data(tableColumns);
    }

    @PostMapping("/execute")
    @Operation(description = "execute")
    public UniformResponse execute(@RequestBody @Parameter(name = "request", description = "sql info") DatasourceExeReqType request) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(request==null || request.id==null || request.sql==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        // find datasource
        DataSourceEntity source = sourceRepository.findById(request.id).get();
        if(source==null){
            // datasource doesn't exist
            return UniformResponse.error(UniformResponseCode.DATASOURCE_NOT_EXIST);
        } else if(!source.getOrg().getId().equals(tokenOrgId) && !tokenIsSuperuser){
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        // replace sql variables
        String selectSqlQuery = SqlUtils.sqlTranslate(request.sql, request.variable);
        if(StrUtil.isEmpty(selectSqlQuery)){
            // not all variables are mapped to exact value
            return UniformResponse.error(UniformResponseCode.SQL_UNKNOWN_VARIABLE);
        }

        // handle dataset field config (filter, sorter and limit)
        // don't rename or hide any column
        List<String> lockedTables = JSONUtil.parseArray(source.getLocked()).toList(String.class);
        selectSqlQuery = SqlUtils.sqlTransfer(selectSqlQuery, source.getType(), lockedTables, request.fields, request.limit, false);
        if(StrUtil.isEmpty(selectSqlQuery)){
            // not all config can be handled
            return UniformResponse.error(UniformResponseCode.DATASET_CONFIG_ERR);
        }

        // verify select query (only select query is supported and prevent SQL Injection Attacks)
        UniformResponseCode validSql = SqlUtils.sqlValidate(selectSqlQuery, source.getType());
        if(validSql!=UniformResponseCode.SUCCESS){
            return UniformResponse.error(UniformResponseCode.SQL_SECURITY_RISK);
        }

        if(!dbUtils.isSourceExist(request.id)){
            // add datasource to manage
            String pwd = new String(Base64.decode(source.getPassword()));
            dbUtils.add(source.getId(), source.getName(), source.getType(), source.getUrl(), source.getParams(), source.getUsername(), pwd);
        }

        List<ColumnField> cols = new ArrayList<>();
        List<Object[]> result = new ArrayList<>();
        List<ColumnField> totalCol = new ArrayList<>();
        List<Object[]> totalRows = new ArrayList<>();
        Integer totalRowCount = null;
        try {
            // get query result
            dbUtils.execute(request.id, selectSqlQuery, cols, result);

            // get total rows of query ignoring limit
            if(source.getType().equalsIgnoreCase("MYSQL")){
                // this is working with MySql to get total records ignoring limit
                dbUtils.execute(request.id, "SELECT FOUND_ROWS();", totalCol, totalRows);
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

        // It will be removed from object if a field is null
        return UniformResponse.ok().data(result, cols, totalRowCount);
    }


    private String testSource(Integer id, String newPassword) {
        String dbVersion = null;
        if(!dbUtils.isSourceExist(id)){
            DataSourceEntity source = sourceRepository.findById(id).get();
            String pwd = new String(Base64.decode(source.getPassword()));
            String newPwd = new String(Base64.decode(newPassword));
            if(!StrUtil.isEmpty(newPwd) && !newPwd.equals("******")){
                dbUtils.add(source.getId(), source.getName(), source.getType(), source.getUrl(), source.getParams(), source.getUsername(), newPwd);
            } else {
                dbUtils.add(source.getId(), source.getName(), source.getType(), source.getUrl(), source.getParams(), source.getUsername(), pwd);
            }
        }

        try {
            dbVersion = dbUtils.testConnection(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        finally {
            return dbVersion;
        }
    }

    private String testSource(String type, String url, JSONArray params, String username, String password) {
        String dbVersion = null;
        dbUtils.add(99999, "connection_test", type, url, params.toString(), username, password);

        try {
            dbVersion = dbUtils.testConnection(99999);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
        finally {
            return dbVersion;
        }
    }

    private List<String> getSourceDbs(Integer id) throws Exception {
        List<String> dbList = null;

        if(!dbUtils.isSourceExist(id)){
            DataSourceEntity source = sourceRepository.findById(id).get();
            String pwd = new String(Base64.decode(source.getPassword()));
            dbUtils.add(source.getId(), source.getName(), source.getType(), source.getUrl(), source.getParams(), source.getUsername(), pwd);
        }

        try {
            dbList = dbUtils.getDatabases(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return dbList;
    }

    private DbTables getSourceTables(Integer id, String dbName) throws Exception {
        DbTables dbTables;

        if(!dbUtils.isSourceExist(id)){
            // get datasource
            DataSourceEntity source = sourceRepository.findById(id).get();
            // decode password
            String pwd = new String(Base64.decode(source.getPassword()));

            // add it to source map
            dbUtils.add(source.getId(), source.getName(), source.getType(), source.getUrl(), source.getParams(), source.getUsername(), pwd);
        }

        try {
            dbTables = dbUtils.getDbTables(id, dbName);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return dbTables;
    }

    private TableColumns getSourceColumns(Integer id, String dbName, String tableName) throws Exception {
        TableColumns tableColumns = null;

        if(!dbUtils.isSourceExist(id)){
            DataSourceEntity source = sourceRepository.findById(id).get();
            String pwd = new String(Base64.decode(source.getPassword()));
            dbUtils.add(source.getId(), source.getName(), source.getType(), source.getUrl(), source.getParams(), source.getUsername(), pwd);
        }

        try {
            tableColumns = dbUtils.getTableColumns(id, dbName, tableName);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return tableColumns;
    }
}
