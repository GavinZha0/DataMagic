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
import com.ninestar.datapie.datamagic.entity.AiAudioEntity;
import com.ninestar.datapie.datamagic.entity.AiModelEntity;
import com.ninestar.datapie.datamagic.repository.AiAudioRepository;
import com.ninestar.datapie.datamagic.repository.AiModelRepository;
import com.ninestar.datapie.datamagic.service.AsyncTaskService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
@RequestMapping("/aiaudio")
@Tag(name="AiAudioApp")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AiAudioController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String FILE_SERVER = System.getProperty("user.dir") + "/fileServer";

    @Resource
    public AiAudioRepository audioAppRepository;

    @Resource
    public AiModelRepository trainedModelRepository;

    @Resource
    private AsyncTaskService asyncService;


    @PostMapping("/list")
    @Operation(summary="getAudioList")
    public UniformResponse getImageList(@RequestBody @Validated TableListReqType req) throws InterruptedException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Long totalRecords = 0L;
        Page<AiAudioEntity> pageEntities = null;
        List<AiAudioEntity> queryEntities = null;

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
        Specification<AiAudioEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenUsername, req.filter, req.search);

        // query data from database
        if(pageable!=null){
            pageEntities = audioAppRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = audioAppRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<AiImageListRspType> rspList = new ArrayList<AiImageListRspType>();
        for(AiAudioEntity entity: queryEntities){
            AiImageListRspType item = new AiImageListRspType();
            BeanUtil.copyProperties(entity, item, new String[]{"model", "content"});
            item.content = new JSONArray(entity.getContent());
            rspList.add(item);
        }


        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", rspList);
        jsonResponse.set("total", totalRecords);
        jsonResponse.set("current", req.page.current);

        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/create")
    @Operation(summary="createImageApp")
    public UniformResponse createImageApp(@RequestBody @Validated AiImageActionReqType req){
        //Hibernate: insert into sys_user (active, avatar, create_time, created_by, deleted, department, email, name, realname, org_id, password, phone, update_time, updated_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.platform) || StrUtil.isEmpty(req.platformVer) || req.content==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<AiAudioEntity> duplicatedEntities = audioAppRepository.findByNameAndGroup(req.name, req.category);
        if(duplicatedEntities.size()>0){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        AiModelEntity modelEntity = trainedModelRepository.findById(req.modelId).get();
        if(modelEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            AiAudioEntity newEntity = new AiAudioEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.description);
            newEntity.setGroup(req.category);
            newEntity.setType(req.type);
            newEntity.setPlatform(req.platform);
            newEntity.setPlatformVer(req.platformVer);
            newEntity.setContent(req.content.toString());
            newEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jp

            // save
            audioAppRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/update")
    @Operation(description = "updateImageApp")
    public UniformResponse updateImageApp(@RequestBody @Parameter(name = "req", description = "Image info") AiImageActionReqType req){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.type) || StrUtil.isEmpty(req.platform) || req.content==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiAudioEntity targetEntity = audioAppRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        AiModelEntity modelEntity = trainedModelRepository.findById(req.modelId).get();
        if(modelEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setId(req.id);
            targetEntity.setName(req.name);
            targetEntity.setType(req.type);
            targetEntity.setDesc(req.description);
            targetEntity.setGroup(req.category);
            targetEntity.setPlatform(req.platform);
            targetEntity.setPlatformVer(req.platformVer);
            targetEntity.setContent(req.content.toString());
            targetEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jpa

            // update source
            audioAppRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.SHARE)
    @PostMapping("/public")
    @Operation(description = "publicImageApp")
    public UniformResponse publicImageApp(@RequestBody @Parameter(name = "params", description = "app id and pub flag") PublicReqType params){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(params.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiAudioEntity targetEntity = audioAppRepository.findById(params.id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            // update public status
            targetEntity.setPubFlag(params.pub);
            audioAppRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/clone")
    @Operation(description = "cloneImageApp")
    public UniformResponse cloneImageApp(@RequestBody @Parameter(name = "param", description = "Image app id") JSONObject param){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(param.get("id").toString());
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiAudioEntity targetEntity = audioAppRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String copyName = targetEntity.getName();
        List<AiAudioEntity> targetCopies = audioAppRepository.findByNameContainingOrderByIdDesc(copyName+"(");
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
            audioAppRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @Operation(description = "deleteImageApp")
    public UniformResponse deleteImageApp(@RequestParam @Parameter(name = "id", description = "Image app id") Integer id){
        //Hibernate: delete from data_source where id=?
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiAudioEntity targetEntity = audioAppRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            // delete entity
            audioAppRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/tree")
    @Operation(description = "getImageTree")
    public UniformResponse getImageTree(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        // jpa page is starting with 0
        List<AiAudioEntity> datasetEntities = audioAppRepository.findAll();

        // convert list to tree by category
        Map<String, List<AiAudioEntity>> datasetMap = datasetEntities.stream().collect(Collectors.groupingBy(t -> t.getGroup()));
        List<TreeSelect> treeDatasets = new ArrayList<>();
        Integer i = 1000;
        for(String group : datasetMap.keySet()){
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for(AiAudioEntity source: datasetMap.get(group)){
                TreeSelect treeNode = new TreeSelect(source.getId(), source.getType(), source.getName(), source.getName(), true, true);
                treeGroup.getChildren().add(treeNode);
            }
            treeDatasets.add(treeGroup);
            i+=100;
        }

        return UniformResponse.ok().data(treeDatasets);
    }

    @PostMapping("/execute")
    @Operation(description = "execute")
    public UniformResponse execute(@RequestBody @Parameter(name = "param", description = "model and target") JSONObject param) throws Exception {
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();
        Integer userId = (Integer)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Integer id = Integer.parseInt(param.get("id").toString());
        Integer modelId = Integer.parseInt(param.get("modelId").toString());
        String fileName = param.get("fileName").toString();

        if(modelId==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiModelEntity marketModel = trainedModelRepository.findById(modelId).get();
        if(marketModel==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String path = FILE_SERVER + "/" + orgId + "/" + userId + "/AI_image/";
        File targetFile = new File(path + fileName);

        if(!targetFile.exists()){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }


        Timestamp ts = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String formatedTS = sdf.format(ts);
        String outputDir = FILE_SERVER + "/" + orgId + "/" + userId + "/AI_image/" + id + "/" + formatedTS;
        String msgTarget = userId + "_image" + id;


        String modelPath = FILE_SERVER+"/public/model/" + marketModel.getArea() + "/" + marketModel.getName();
        List<String> fileList = null;
        if(!StrUtil.isEmpty(marketModel.getArea())){
            fileList = JSONUtil.parseArray(marketModel.getArea()).toList(String.class);
        }

        // run DL4J/DJL example
        CompletableFuture<Integer> future = null;
        logger.info("Start a thread to execute DJL model, inform UI to start progressbar via stomp......");
        switch (marketModel.getArea().toLowerCase()){
            case "djl": {
                future = asyncService.executeDJL(modelPath, path + fileName, outputDir, msgTarget);
                break;
            }
            default: {
                future = asyncService.executeDJL(modelPath, fileList, marketModel.getArea(), path + fileName, outputDir, msgTarget);
                break;
            }
        }

        // callback after the task is finished
        future.thenAccept((ret)->{
            logger.info("DJL thread is over with return {}, inform UI to stop progressbar via stomp......", ret);
        });
        // non-blocking return
        return UniformResponse.ok();

    }

    @PostMapping("/category")
    @Operation(description = "getCatOptions")
    public UniformResponse getCatOptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        Set<Object> distinctCategory = audioAppRepository.findDistinctGroup();
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
        String path = projectDir + "/fileServer/" + orgId + "/" + userId + "/AI_audio/";

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
