package com.ninestar.datapie.datamagic.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.AiImageEntity;
import com.ninestar.datapie.datamagic.entity.AiModelEntity;
import com.ninestar.datapie.datamagic.repository.AiImageRepository;
import com.ninestar.datapie.datamagic.repository.AiModelRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
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
@RequestMapping("/aimodel")
@Tag(name = "AiTrainedModel")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AiModelController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    public AiModelRepository trainedModelRepository;

    @Resource
    public AiImageRepository imageAppRepository;

    @PostMapping("/list")
    @Operation(description = "getModels")
    public UniformResponse getModels(@RequestBody @Parameter(name = "req", description = "request") TableListReqType req) throws InterruptedException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Long totalRecords = 0L;
        Page<AiModelEntity> pageEntities = null;
        List<AiModelEntity> queryEntities = null;

        // put multiple orders into a sort which will be put into a pageable
        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort sortable = null;
        Pageable pageable = null;

        // build sort object
        //support multiple orders
        if(req.sorter!=null && req.sorter.orders.length>0){
            for(int i = 0; i< req.sorter.fields.length; i++){
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
        Specification<AiModelEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenUsername, req.filter, req.search);

        // query data from database
        if(pageable!=null){
            pageEntities = trainedModelRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = trainedModelRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<AiModelListRspType> rspList = new ArrayList<AiModelListRspType>();
        for(AiModelEntity entity: queryEntities){
            AiModelListRspType item = new AiModelListRspType();
            BeanUtil.copyProperties(entity, item, new String[]{"tags", "content", "input", "output", "evaluation", "detail"});
            if(!StrUtil.isEmpty(entity.getTags())) {
                // convert string to array
                item.tags = JSONUtil.parseArray(entity.getTags()).toList(String.class);
            }
            if(!StrUtil.isEmpty(entity.getFiles())) {
                // convert string to array
                item.files = JSONUtil.parseArray(entity.getFiles()).toList(String.class);
            }
            if(!StrUtil.isEmpty(entity.getInput())) {
                item.input = new JSONArray(entity.getInput());
            }
            if(!StrUtil.isEmpty(entity.getOutput())) {
                item.output = new JSONArray(entity.getOutput());
            }
            if(!StrUtil.isEmpty(entity.getEval())) {
                item.evaluation = new JSONArray(entity.getEval());
            }
            if(!StrUtil.isEmpty(entity.getDetail())) {
                item.detail = new JSONObject(entity.getDetail());
            }

            if(entity.getScore()!=null){
                item.rate = entity.getScore().floatValue()/2;
            }

            // get model usage
            List<AiImageEntity> images = imageAppRepository.findByModelId(entity.getId());
            item.usage = images.size();

            rspList.add(item);
        }


        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", rspList);
        jsonResponse.set("total", totalRecords);
        jsonResponse.set("current", req.page.current);

        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/create")
    @Operation(description = "createModel")
    public UniformResponse createModel(@RequestBody @Parameter(name = "req", description = "model info") AiModelActionReqType req){
        //Hibernate: insert into sys_user (active, avatar, create_time, created_by, deleted, department, email, name, realname, org_id, password, phone, update_time, updated_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.category) || StrUtil.isEmpty(req.type) || StrUtil.isEmpty(req.framework)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<AiModelEntity> duplicatedEntities = trainedModelRepository.findByNameAndCategoryAndType(req.name, req.category, req.type);
        if(duplicatedEntities.size()!=0){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        try {
            AiModelEntity newEntity = new AiModelEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.description);
            newEntity.setCategory(req.category);
            newEntity.setType(req.type);
            newEntity.setFramework(req.framework);
            newEntity.setFrameVer(req.frameVer);
            newEntity.setNetwork(req.network);
            newEntity.setTrainset(req.trainset);
            newEntity.setPrice(req.price);
            //newEntity.setDetail(req.detail.toString());
            newEntity.setWeblink(req.weblink);
            newEntity.setVersion(req.version);
            newEntity.setModelId(req.modelId);

            newEntity.setTags(req.tags.toString());
            newEntity.setEval(req.eval.toString());
            newEntity.setInput(req.input.toString());
            newEntity.setOutput(req.output.toString());
            newEntity.setFiles(req.files.toString());

            newEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jp

            // save source
            trainedModelRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/update")
    @Operation(description = "updateModel")
    public UniformResponse updateModel(@RequestBody @Parameter(name = "req", description = "model info") AiModelActionReqType req){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.category) || StrUtil.isEmpty(req.type) || StrUtil.isEmpty(req.framework)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiModelEntity targetEntity = trainedModelRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setName(req.name);
            targetEntity.setDesc(req.description);
            targetEntity.setCategory(req.category);
            targetEntity.setType(req.type);
            targetEntity.setFramework(req.framework);
            targetEntity.setFrameVer(req.frameVer);
            targetEntity.setNetwork(req.network);
            targetEntity.setTrainset(req.trainset);
            targetEntity.setPrice(req.price);
            //targetEntity.setDetail(req.detail.toString());
            targetEntity.setWeblink(req.weblink);
            targetEntity.setVersion(req.version);
            targetEntity.setModelId(req.modelId);

            targetEntity.setTags(req.tags.toString());
            targetEntity.setEval(req.eval.toString());
            targetEntity.setInput(req.input.toString());
            targetEntity.setOutput(req.output.toString());
            targetEntity.setFiles(req.files.toString());

            targetEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jpa

            // update source
            trainedModelRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.SHARE)
    @PostMapping("/public")
    @Operation(description = "publicModel")
    public UniformResponse publicModel(@RequestBody @Parameter(name = "params", description = "model id and pub flag") PublicReqType params){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(params.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiModelEntity targetEntity = trainedModelRepository.findById(params.id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            // update public status
            targetEntity.setPubFlag(params.pub);
            trainedModelRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/clone")
    @Operation(description = "cloneModel")
    public UniformResponse cloneModel(@RequestBody @Parameter(name = "param", description = "model id") JSONObject param){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(param.get("id").toString());
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiModelEntity targetEntity = trainedModelRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String copyName = targetEntity.getName();
        List<AiModelEntity> targetCopies = trainedModelRepository.findByNameContainingOrderByIdDesc(copyName+"(");
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
            trainedModelRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @Operation(description = "deleteModel")
    public UniformResponse deleteModel(@RequestParam @Parameter(name = "id", description = "model id") Integer id){
        //Hibernate: delete from data_source where id=?
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiModelEntity targetEntity = trainedModelRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            // delete entity
            trainedModelRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/tree")
    @Operation(description = "getModelTree")
    public UniformResponse getModelTree(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        // jpa page is starting with 0
        List<AiModelEntity> datasetEntities = trainedModelRepository.findAll();

        // convert list to tree by category
        Map<String, List<AiModelEntity>> datasetMap = datasetEntities.stream().collect(Collectors.groupingBy(t -> t.getCategory()));
        List<TreeSelect> treeDatasets = new ArrayList<>();
        Integer i = 1000;
        for(String group : datasetMap.keySet()){
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for(AiModelEntity source: datasetMap.get(group)){
                TreeSelect treeNode = new TreeSelect(source.getId(), source.getType(), source.getName(), source.getName(), true, true);
                treeGroup.getChildren().add(treeNode);
            }
            treeDatasets.add(treeGroup);
            i+=100;
        }

        return UniformResponse.ok().data(treeDatasets);
    }


    @PostMapping("/types")
    @Operation(description = "getTypeOptions")
    public UniformResponse getTypeOptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        Set<Object> distinctType = trainedModelRepository.findDistinctType();
        Set<OptionsRspType> catSet = new HashSet<>();

        Integer i = 0;
        // get distinct category set
        for(Object item: distinctType){
            OptionsRspType cat = new OptionsRspType();
            cat.id = i;
            cat.name = item.toString();
            catSet.add(cat);
            i++;
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", catSet);
        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/category")
    @Operation(description = "getCatOptions")
    public UniformResponse getCatOptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        Set<Object> distinctCategory = trainedModelRepository.findDistinctCategory();
        Set<OptionsRspType> catSet = new HashSet<>();

        Integer i = 0;
        // get distinct category set
        for(Object item: distinctCategory){
            OptionsRspType cat = new OptionsRspType();
            cat.id = i;
            cat.name = item.toString();
            catSet.add(cat);
            i++;
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", catSet);
        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/upload")
    @Operation(description = "imageUpload")
    public UniformResponse imageUpload(@RequestParam("files") MultipartFile[] files) throws Exception {
        //Hibernate: insert into sys_user (active, avatar, create_time, created_by, deleted, department, email, name, realname, org_id, password, phone, update_time, updated_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loginUser = auth.getCredentials().toString();
        String orgId = auth.getDetails().toString();
        String  userId = auth.getPrincipal().toString();

        if(files==null || files.length<=0 ){
            return UniformResponse.error();
        }

        // file server is under project temporarily
        // later it should be replaced by a real file server like minio or cloud storage solution
        String projectDir = System.getProperty("user.dir");
        String path = projectDir + "/fileServer/" + orgId + "/" + userId + "/AI_image/";

        File targetFolder = new File(path);
        Boolean forderReady =false;
        if (!targetFolder.exists()) {
            forderReady = targetFolder.mkdirs();
        }
        else{
            forderReady = true;
        }

        if(forderReady){
            for(int i=0; i<files.length; i++) {
                try {
                    files[i].transferTo(new File(targetFolder + "/" + files[i].getOriginalFilename()));
                }catch (Exception e){
                    e.printStackTrace();
                    logger.error(e.getMessage());
                    return UniformResponse.error(e.getMessage());
                }
            }
        }

        return UniformResponse.ok();
    }
}
