package com.ninestar.datapie.datamagic.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.config.RedisConfig;
import com.ninestar.datapie.datamagic.entity.MlDatasetEntity;
import com.ninestar.datapie.datamagic.entity.MlEdaEntity;
import com.ninestar.datapie.datamagic.repository.MlDatasetRepository;
import com.ninestar.datapie.datamagic.repository.MlEdaRepository;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.TreeSelect;
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
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;
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
@RequestMapping("/ml/eda")
@Tag(name = "MlEda")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MlEdaController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private RedisConfig redisConfig;
    @Resource
    private StringRedisTemplate redisTpl;

    @Resource
    public MlDatasetRepository datasetRepository;

    @Resource
    public MlEdaRepository edaRepository;

    @Resource
    public SysOrgRepository orgRepository;

    @PostMapping("/list")
    @Operation(description = "getEdaList")
    public UniformResponse getEdaList(@RequestBody @Parameter(name = "req", description = "request") TableListReqType req) throws InterruptedException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Long totalRecords = 0L;
        Page<MlEdaEntity> pageEntities = null;
        List<MlEdaEntity> queryEntities = null;

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
        Specification<MlEdaEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenUsername, req.filter, req.search);

        // query data from database
        if(pageable!=null){
            pageEntities = edaRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = edaRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<MlEdaListRspType> rspList = new ArrayList<MlEdaListRspType>();
        for(MlEdaEntity entity: queryEntities){
            MlEdaListRspType item = new MlEdaListRspType();
            BeanUtil.copyProperties(entity, item, new String[]{"fields", "config"});
            item.datasetId = entity.getDataset().getId();
            item.datasetName = entity.getDataset().getGroup() + "/" + entity.getDataset().getName();
            if(StrUtil.isNotEmpty(entity.getConfig())){
                item.config = new JSONObject(entity.getConfig());
            }

            rspList.add(item);
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", rspList);
        jsonResponse.set("total", totalRecords);
        jsonResponse.set("current", req.page.current);

        return UniformResponse.ok().data(jsonResponse);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @Operation(description = "createEda")
    public UniformResponse createEda(@RequestBody @Parameter(name = "req", description = "dataset info") MlEdaActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(req.name) || req.datasetId<1){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<MlEdaEntity> duplicatedEntities = edaRepository.findByNameAndGroup(req.name, req.group);
        if(duplicatedEntities.size()>0){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        MlDatasetEntity dataset = datasetRepository.findById(req.datasetId).get();
        if(dataset==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        try {
            MlEdaEntity newEntity = new MlEdaEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.desc);
            newEntity.setGroup(req.group);
            newEntity.setDataset(dataset);
            if(req.config!=null){
                newEntity.setConfig(req.config.toString());
            }

            if(req.pubFlag==null){
                newEntity.setPubFlag(false);
            }
            else{
                newEntity.setPubFlag(req.pubFlag);
            }

            newEntity.setOrg(orgRepository.findById(tokenOrgId).get());
            //create_time and update_time are generated automatically by jp

            // save source
            edaRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @Operation(description = "updateEda")
    public UniformResponse updateEda(@RequestBody @Parameter(name = "req", description = "Eda info") MlEdaActionReqType req){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || req.datasetId<1){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlEdaEntity targetEntity = edaRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        MlDatasetEntity dataset = datasetRepository.findById(req.datasetId).get();
        if(dataset==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        try {
            targetEntity.setId(req.id);
            targetEntity.setName(req.name);
            targetEntity.setDesc(req.desc);
            targetEntity.setGroup(req.group);
            targetEntity.setDataset(dataset);
            if(req.config!=null){
                targetEntity.setConfig(req.config.toString());
            } else {
                targetEntity.setConfig(null);
            }

            targetEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jpa

            // update source
            edaRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.SHARE)
    @PostMapping("/public")
    @Operation(description = "publicEda")
    public UniformResponse publicEda(@RequestBody @Parameter(name = "params", description = "dataset id and pub flag") PublicReqType params){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(params.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlEdaEntity targetEntity = edaRepository.findById(params.id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            // update public status
            targetEntity.setPubFlag(params.pub);
            edaRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.CLONE)
    @PostMapping("/clone")
    @Operation(description = "cloneEda")
    public UniformResponse cloneEda(@RequestBody @Parameter(name = "param", description = "dataset id") JSONObject param){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        Integer id = Integer.parseInt(param.get("id").toString());
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlEdaEntity targetEntity = edaRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String copyName = targetEntity.getName();
        List<MlEdaEntity> targetCopies = edaRepository.findByNameContainingOrderByIdDesc(copyName+"(");
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
            edaRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @Operation(description = "deleteEda")
    public UniformResponse deleteEda(@RequestParam @Parameter(name = "id", description = "dataset id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlEdaEntity targetEntity = edaRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            // delete entity
            edaRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/tree")
    @Operation(description = "getEdaTree")
    public UniformResponse getEdaTree(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        // jpa page is starting with 0
        List<MlEdaEntity> datasetEntities = edaRepository.findAll();

        // convert list to tree by category
        Map<String, List<MlEdaEntity>> datasetMap = datasetEntities.stream().collect(Collectors.groupingBy(t -> t.getGroup()));
        List<TreeSelect> treeDatasets = new ArrayList<>();
        Integer i = 1000;
        for(String group : datasetMap.keySet()){
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for(MlEdaEntity source: datasetMap.get(group)){
                TreeSelect treeNode = new TreeSelect(source.getId(), source.getGroup(), source.getName(), source.getName(), true, true);
                treeGroup.getChildren().add(treeNode);
            }
            treeDatasets.add(treeGroup);
            i+=100;
        }

        return UniformResponse.ok().data(treeDatasets);
    }

    @PostMapping("/getone")
    @Operation(description = "getEda")
    public UniformResponse getEda(@RequestBody @Parameter(name = "param", description = "dataset id") JSONObject param){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        Integer id = Integer.parseInt(param.get("id").toString());

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlEdaEntity targetEntity = edaRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        DatasetListRspType response = new DatasetListRspType();
        BeanUtil.copyProperties(targetEntity, response);
        return UniformResponse.ok().data(response);
    }

    @PostMapping("/execute")
    @Operation(description = "execute")
    public UniformResponse execute(@RequestBody @Parameter(name = "param", description = "Eda id") JSONObject param) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        String taskId;

        Integer id = Integer.parseInt(param.get("id").toString());

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlEdaEntity targetEntity = edaRepository.findById(id).get();
        if(targetEntity==null || targetEntity.getDataset()==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        // control plane
        // forward command to python server for user x and algorithm y
        try{
            String uniqueId = tokenUserId.toString() + "_ml_feature_"+targetEntity.getId();
            // save to parameters to redis
            redisTpl.opsForValue().set(uniqueId, targetEntity.toString());
            // send req to channel
            redisTpl.convertAndSend("downstream", targetEntity);

            StringRecord stringRecord = StreamRecords.string(Collections.singletonMap("data", targetEntity.toString())).withStreamKey(redisConfig.getReqStream());
            RecordId rId = redisTpl.opsForStream().add(stringRecord);
            taskId = rId.toString();
            System.out.println("Send task: " + taskId);

        }catch (Exception e){
            logger.error(e.getMessage());
            return UniformResponse.error(e.getMessage());
        }

        JSONObject rspObj = new JSONObject();
        rspObj.set("taskId", taskId);
        return UniformResponse.ok().data(rspObj);
    }

    @PostMapping("/groups")
    @Operation(description = "getGroups")
    public UniformResponse getGroups() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        List<Object> distinctGroups = edaRepository.findDistinctGroup();
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", distinctGroups);
        return UniformResponse.ok().data(jsonResponse);
    }
}
