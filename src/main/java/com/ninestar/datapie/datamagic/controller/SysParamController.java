package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.ParamActionReqType;
import com.ninestar.datapie.datamagic.bridge.TableListReqType;
import com.ninestar.datapie.datamagic.entity.SysParamEntity;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.repository.SysParamRepository;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
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
import java.util.ArrayList;
import java.util.List;
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
@RequestMapping("/param")
@Tag(name = "SysParam")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SysParamController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysParamRepository configRepository;

    @Resource
    private SysOrgRepository orgRepository;

    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @Operation(summary = "getConfigList")
    public UniformResponse getConfigList(@RequestBody @Parameter(description = "request") TableListReqType request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Long totalRecords = 0L;
        Page<SysParamEntity> pageEntities = null;
        List<SysParamEntity> queryEntities = null;

        // put multiple orders into a sort which will be put into a pageable
        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort sortable = null;
        Pageable pageable = null;

        // build sort object
        //support multiple orders
        if(request.sorter!=null && request.sorter.orders.length>0){
            for(int i=0; i< request.sorter.fields.length; i++){
                Sort.Order order = null;
                if(request.sorter.orders[i].equalsIgnoreCase("ascend")){
                    order = new Sort.Order(Sort.Direction.ASC, request.sorter.fields[i]);
                }
                else{
                    order = new Sort.Order(Sort.Direction.DESC, request.sorter.fields[i]);
                }
                orders.add(order);
            }
            sortable = Sort.by(orders);
        }

        // build page object with/without sort
        if(request.page!=null){
            // jpa page starts from 0
            if(request.sorter!=null && request.sorter.fields.length>0){
                pageable = PageRequest.of(request.page.current-1, request.page.pageSize, sortable);
            }
            else{
                pageable = PageRequest.of(request.page.current-1, request.page.pageSize);
            }
        }

        // build JPA specification
        Specification<SysParamEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenIsAdmin, tokenUsername, request.filter, request.search);

        // query data from database
        if(pageable!=null){
            pageEntities = configRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = configRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", queryEntities);
        jsonResponse.set("total", totalRecords);
        jsonResponse.set("current", request.page.current);

        return UniformResponse.ok().data(jsonResponse);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @Operation(summary = "createConfig")
    public UniformResponse createConfig(@RequestBody @Parameter(description = "config info") ParamActionReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(StrUtil.isEmpty(request.name) || StrUtil.isEmpty(request.module) || StrUtil.isEmpty(request.group) || StrUtil.isEmpty(request.value)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysParamEntity duplicatedEntity = configRepository.findByName(request.name);
        if(duplicatedEntity!=null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        try {
            SysParamEntity newParam = new SysParamEntity();
            newParam.setName(request.name);
            newParam.setDesc(request.desc);
            newParam.setModule(request.module);
            newParam.setType(request.type);
            newParam.setGroup(request.group);
            newParam.setValue(request.value);
            newParam.setOrg(orgRepository.findById(tokenOrgId).get());

            // save source
            configRepository.save(newParam);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @Operation(summary = "updateConfig")
    public UniformResponse updateConfig(@RequestBody @Parameter(name = "source", description = "source info") ParamActionReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(request.id==0 || StrUtil.isEmpty(request.name)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysParamEntity targetEntity = configRepository.findByName(request.name);
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setName(request.name);

            targetEntity.setDesc(request.desc);
            targetEntity.setModule(request.module);
            targetEntity.setGroup(request.group);
            targetEntity.setType(request.type);
            targetEntity.setPrevious(targetEntity.getValue()); // save previous value
            targetEntity.setValue(request.value);
            //create_time and update_time are generated automatically by jpa

            // update source
            configRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/get")
    @Operation(summary = "getParameter")
    public UniformResponse getParameter(@RequestBody @Parameter(description = "parameter name") JSONObject request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        String paramName = request.get("name").toString();

        if(StrUtil.isEmpty(paramName)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        // get parameter by name
        SysParamEntity entity = null;
        String[] params = paramName.split("[.]");
        if(params.length==1){
            // only name
            entity = configRepository.findByName(paramName);
        }
        else if(params.length==2){
            //group.name
            entity = configRepository.findByGroupAndName(params[0], params[1]);
        }
        else if(params.length>2){
            //module.group.name
            entity = configRepository.findByModuleAndGroupAndName(params[params.length-3], params[params.length-2], params[params.length-1]);
        }

        if(entity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String valueType = entity.getType();
        JSONObject jsonResponse = new JSONObject();
        //jsonResponse.set("id", entity.getId());
        jsonResponse.set("name", entity.getName());

        if(valueType.toUpperCase() == "STRING"){
            jsonResponse.set("value", entity.getValue());
            return UniformResponse.ok().data(jsonResponse);
        }
        else if(valueType.toUpperCase() == "NUMBER"){
            jsonResponse.set("value", Integer.parseInt(entity.getValue()));
            return UniformResponse.ok().data(jsonResponse);
        }
        else if(valueType.toUpperCase() == "BOOLEAN"){
            jsonResponse.set("value", Boolean.parseBoolean(entity.getValue()));
            return UniformResponse.ok().data(jsonResponse);
        }
        else if(valueType.toUpperCase() == "JSON"){
            jsonResponse.set("value", new JSONObject(entity.getValue()));
            return UniformResponse.ok().data(jsonResponse);
        }
        else if(valueType.toUpperCase().startsWith("[")){
            if(valueType.toUpperCase().contains("STRING")){
                List<String> valueList = JSONUtil.parseArray(entity.getValue()).toList(String.class);
                jsonResponse.set("value", valueList);
                return UniformResponse.ok().data(jsonResponse);
            }
            else if(valueType.toUpperCase().contains("NUMBER")){
                List<Number> valueList = JSONUtil.parseArray(entity.getValue()).toList(Number.class);
                jsonResponse.set("value", valueList);
                return UniformResponse.ok().data(jsonResponse);
            }
            else if(valueType.toUpperCase().contains("BOOLEAN")){
                List<Boolean> valueList = JSONUtil.parseArray(entity.getValue()).toList(Boolean.class);
                jsonResponse.set("value", valueList);
                return UniformResponse.ok().data(jsonResponse);
            }
            else if(valueType.toUpperCase().contains("JSON")){
                jsonResponse.set("value", new JSONArray(entity.getValue()));
                return UniformResponse.ok().data(jsonResponse);
            }
        }

        return UniformResponse.ok().data(entity);
    }
}
