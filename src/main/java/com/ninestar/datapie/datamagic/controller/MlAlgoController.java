package com.ninestar.datapie.datamagic.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.config.RedisConfig;
import com.ninestar.datapie.datamagic.entity.MlAlgoEntity;
import com.ninestar.datapie.datamagic.entity.MlDatasetEntity;
import com.ninestar.datapie.datamagic.repository.MlAlgoRepository;
import com.ninestar.datapie.datamagic.repository.MlDatasetRepository;
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
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
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
@RequestMapping("/ml/algo")
@Tag(name = "MlAlgorithm")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MlAlgoController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String pyServerUrl = "http://localhost:9538/ml/";

    @Resource
    public MlDatasetRepository datasetRepository;
    @Resource
    public MlAlgoRepository algorithmRepository;

    @Resource
    public SysOrgRepository orgRepository;

    @Resource
    public RedisConfig redisConfig;

    @Resource
    public RedisTemplate redisTemplate;

    @PostMapping("/list")
    @Operation(description = "getAlgorithmList")
    public UniformResponse getAlgorithmList(@RequestBody @Parameter(name = "req", description = "request") TableListReqType req) throws InterruptedException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Long totalRecords = 0L;
        Page<MlAlgoEntity> pageEntities = null;
        List<MlAlgoEntity> queryEntities = null;

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
        Specification<MlAlgoEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenUsername, req.filter, req.search);

        // query data from database
        if(pageable!=null){
            pageEntities = algorithmRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = algorithmRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<AlgorithmListRspType> rspList = new ArrayList<AlgorithmListRspType>();
        for(MlAlgoEntity entity: queryEntities){
            AlgorithmListRspType item = new AlgorithmListRspType();
            BeanUtil.copyProperties(entity, item, new String[]{"dataCfg", "trainCfg"});
            item.dataCfg = new JSONObject(entity.getDataCfg());
            item.trainCfg = new JSONObject(entity.getTrainCfg());
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
    @Operation(description = "createAlgorithm")
    public UniformResponse createAlgorithm(@RequestBody @Parameter(name = "req", description = "dataset info") AlgorithmActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.framework) || StrUtil.isEmpty(req.srcCode)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        try {
            MlAlgoEntity newEntity = new MlAlgoEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.desc);
            newEntity.setGroup(req.group);
            newEntity.setCategory(req.category);
            newEntity.setAlgoName(req.algoName);
            newEntity.setFramework(req.framework);
            newEntity.setFrameVer("3.11");
            newEntity.setSrcCode(req.srcCode);
            if(req.dataCfg!=null){
                newEntity.setDataCfg(req.dataCfg.toString());
            }
            if(req.trainCfg!=null){
                newEntity.setTrainCfg(req.trainCfg.toString());
            }
            newEntity.setPubFlag(false);
            newEntity.setOrg(orgRepository.findById(tokenOrgId).get());
            //create_time and update_time are generated automatically by jp

            // save source
            algorithmRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @Operation(description = "updateAlgorithm")
    public UniformResponse updateAlgorithm(@RequestBody @Parameter(name = "req", description = "Algorithm info") AlgorithmActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");


        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.framework) || StrUtil.isEmpty(req.srcCode)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlAlgoEntity targetEntity = algorithmRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setId(req.id);
            targetEntity.setName(req.name);
            targetEntity.setCategory(req.category);
            targetEntity.setAlgoName(req.algoName);
            targetEntity.setDesc(req.desc);
            targetEntity.setGroup(req.group);
            targetEntity.setFramework(req.framework);
            targetEntity.setFrameVer("3.11");
            targetEntity.setDataCfg(req.dataCfg.toString());
            targetEntity.setTrainCfg(req.trainCfg.toString());
            targetEntity.setSrcCode(req.srcCode);
            targetEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jpa

            // update source
            algorithmRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.SHARE)
    @PostMapping("/public")
    @Operation(description = "publicAlgorithm")
    public UniformResponse publicAlgorithm(@RequestBody @Parameter(name = "params", description = "dataset id and pub flag") PublicReqType params){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");


        if(params.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlAlgoEntity targetEntity = algorithmRepository.findById(params.id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            // update public status
            targetEntity.setPubFlag(params.pub);
            algorithmRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.CLONE)
    @PostMapping("/clone")
    @Operation(description = "cloneALgorithm")
    public UniformResponse cloneALgorithm(@RequestBody @Parameter(name = "param", description = "dataset id") JSONObject param){
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

        MlAlgoEntity targetEntity = algorithmRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String copyName = targetEntity.getName();
        List<MlAlgoEntity> targetCopies = algorithmRepository.findByNameContainingOrderByIdDesc(copyName+"(");
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
            algorithmRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @Operation(description = "deleteAlgorithm")
    public UniformResponse deleteAlgorithm(@RequestParam @Parameter(name = "id", description = "dataset id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");


        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlAlgoEntity targetEntity = algorithmRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            // delete entity
            algorithmRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/tree")
    @Operation(description = "getAlgorithmTree")
    public UniformResponse getAlgorithmTree(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");


        // jpa page is starting with 0
        List<MlAlgoEntity> datasetEntities = algorithmRepository.findAll();

        // convert list to tree by category
        Map<String, List<MlAlgoEntity>> datasetMap = datasetEntities.stream().collect(Collectors.groupingBy(t -> t.getGroup()));
        List<TreeSelect> treeDatasets = new ArrayList<>();
        Integer i = 1000;
        for(String group : datasetMap.keySet()){
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for(MlAlgoEntity source: datasetMap.get(group)){
                TreeSelect treeNode = new TreeSelect(source.getId(), source.getCategory(), source.getName(), source.getName(), true, true);
                treeGroup.getChildren().add(treeNode);
            }
            treeDatasets.add(treeGroup);
            i+=100;
        }

        return UniformResponse.ok().data(treeDatasets);
    }

    @PostMapping("/getone")
    @Operation(description = "getAlgorithm")
    public UniformResponse getAlgorithm(@RequestBody @Parameter(name = "param", description = "dataset id") JSONObject param){
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

        MlAlgoEntity targetEntity = algorithmRepository.findById(id).get();
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
    public UniformResponse execute(@RequestBody @Parameter(name = "param", description = "algorithm id") JSONObject param) throws Exception {
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

        MlAlgoEntity targetEntity = algorithmRepository.findById(id).get();
        if(targetEntity==null || targetEntity.getSrcCode()==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        List<String> frames = Arrays.asList("python", "sklearn", "pytorch", "tensorflow");
        if(frames.contains(targetEntity.getFramework())){
            // forward command to python server for user x and algorithm y
            try{
                Map<String, Object> taskMap = BeanUtil.beanToMap(targetEntity);
                taskMap.put("userId", tokenUserId);
                taskMap.put("task", "ml.algo." + targetEntity.getId());

                // send msg to python server via stream of redis
                MapRecord stringRecord = StreamRecords.newRecord().ofMap(taskMap).withStreamKey(redisConfig.getReqStream());
                RecordId msgId = redisTemplate.opsForStream().add(stringRecord);
                System.out.println("Send task to py server via redis stream: " + msgId.getValue());
            }catch (Exception e){
                logger.error(e.getMessage());
                return UniformResponse.error(e.getMessage());
            }
        } else {
            // schedule a aync task
        }

        return UniformResponse.ok();
    }

    @PostMapping("/execute_script")
    @Operation(description = "executeScript")
    public UniformResponse executeScript(@RequestBody @Parameter(name = "request", description = "request info") AlgorithmActionReqType request) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");


        if(request==null || request.id==null || StrUtil.isEmpty(request.srcCode) || StrUtil.isEmpty(request.framework)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        String pythonServerUrl = "http://localhost:9538/ml/execute_script";
        HttpResponse response = HttpRequest.post(pythonServerUrl)
                .body(JSONUtil.parseObj(request).toString())
                .execute();
        JSONObject result = new JSONObject(response.body());

        return result.toBean(UniformResponse.class);
        //return UniformResponse.ok().data(result);
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

        List<Object> distinctGroups = algorithmRepository.findGroupsInOrgId(tokenOrgId);
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", distinctGroups);
        return UniformResponse.ok().data(jsonResponse);
    }
}
