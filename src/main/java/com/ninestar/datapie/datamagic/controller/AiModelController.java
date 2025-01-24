package com.ninestar.datapie.datamagic.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.config.MLflowConfig;
import com.ninestar.datapie.datamagic.entity.AiModelEntity;
import org.springframework.beans.factory.annotation.Value;
import com.ninestar.datapie.datamagic.entity.MlDatasetEntity;
import com.ninestar.datapie.datamagic.repository.AiModelRepository;
import com.ninestar.datapie.datamagic.repository.MlAlgoRepository;
import com.ninestar.datapie.datamagic.repository.MlDatasetRepository;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.utils.DbUtils;
import com.ninestar.datapie.datamagic.utils.JwtTokenUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.ColumnField;
import com.ninestar.datapie.framework.model.TreeSelect;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;

/**
 * <p>
 *  Controller
 * </p>
 *
 * @author Gavin.Zhao
 * @since 2021-09-18
 */
@RestController
@RequestMapping("/ai/model")
@Tag(name = "AiModel")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AiModelController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${server.py.url}")
    private String pyServerUrl;

    @Value("${server.mlflow.endpoint}")
    private String mlflowServerEndpoint;

    @Resource
    public AiModelRepository modelRepository;

    @Resource
    private DbUtils dbUtils;

    @Resource
    private MLflowConfig mLflowConfig;

    @Resource
    private MlAlgoRepository mlAlgoRepository;

    @Resource
    private MlDatasetRepository mlDatasetRepository;

    @Resource
    private SysOrgRepository orgRepository;


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
            pageEntities = modelRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = modelRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<AiModelListRspType> rspList = new ArrayList<AiModelListRspType>();
        List<String> runIds = new ArrayList<>();
        for(AiModelEntity entity: queryEntities){
            AiModelListRspType item = new AiModelListRspType();
            BeanUtil.copyProperties(entity, item, new String[]{"transform", "schema", "tags"});
            if(!StrUtil.isEmpty(entity.getTags())) {
                // convert string to array
                item.tags = JSONUtil.parseArray(entity.getTags()).toList(String.class);
                // fields transform which is from ML dataset
                item.transform = new JSONArray(entity.getTransform());
                // schema includes inputs and outputs
                item.schema = new JSONObject(entity.getSchema());
            }

            runIds.add(entity.getRunId());
            rspList.add(item);
        }

        if(runIds.size() > 0){
            JSONArray mlflowRecords = new JSONArray();
            try {
                // find registered models in mlflow db
                mlflowRecords = listRegisteredModels("'" + String.join("','", runIds) + "'");
            } catch (Exception e){
                logger.error(e.getMessage());
                return UniformResponse.error(UniformResponseCode.API_EXCEPTION_SQL_EXE);
            }

            for(AiModelListRspType item: rspList){
                // Find it by run id and version
                JSONObject registeredMoel = mlflowRecords.stream()
                        .map(JSONObject.class::cast)
                        .filter(obj -> obj.getStr("run_uuid").equals(item.runId) && Objects.equals(obj.getInt("version"), item.version))
                        .findFirst()
                        .orElse(null);

                // append registered info of mlflow to response
                if(registeredMoel != null){
                    JSONObject reg = new JSONObject(registeredMoel.get("register"));
                    item.algoName = reg.get("algoName").toString();

                    item.trainedAt = Timestamp.from(Instant.ofEpochMilli(Long.parseLong(registeredMoel.get("trained_at").toString())));
                    item.trainedBy = registeredMoel.get("trained_by").toString();


                    JSONObject evals = new JSONObject(registeredMoel.get("metrics"));
                    if(evals.get("accuracy_score") != null){
                        item.eval = "Accuracy: " + evals.get("accuracy_score").toString();
                    }

                    item.metrics = registeredMoel.get("metrics");
                    // schema is in model definition (no longer to get from mlflow)
                    // item.schema = registeredMoel.get("in_schema");
                }
            }
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.area)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<AiModelEntity> duplicatedEntities = modelRepository.findByNameAndAreaAndVersion(req.name, req.area, req.version);
        if(duplicatedEntities.size()!=0){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        // default value
        if(StrUtil.isEmpty(req.deployTo)){
            req.deployTo = "MLflow";
        }

        if(StrUtil.isEmpty(req.endpoint)){
            req.endpoint = mlflowServerEndpoint;
        }

        if(StrUtil.isEmpty(req.price)){
            req.price = "0";
        }

        // get data transform from dataset
        String dataTransform = null;
        if(req.datasetId!=null){
            MlDatasetEntity datasetEntity = mlDatasetRepository.findById(req.datasetId).get();
            dataTransform = datasetEntity.getFields();
        }

        // create a new token
        AuthLoginRspType userInfo = new AuthLoginRspType();
        userInfo.id = tokenUserId;
        userInfo.name = tokenUsername;
        userInfo.orgId = tokenOrgId;
        String token = JwtTokenUtil.createToken(userInfo, null);

        // build request parameters
        JSONObject pyParams = new JSONObject();
        pyParams.set("run_id", req.runId);

        // send http request to python server to get model schema
        HttpResponse response = HttpRequest.post(pyServerUrl + "/ai/model/schema")
                .header("authorization", "Bearer " + token)
                .body(pyParams.toString())
                .execute();

        // decode response of python server
        JSONObject result = new JSONObject(response.body());
        String schema = null;
        UniformResponse pyRsp = result.toBean(UniformResponse.class);
        if(pyRsp.getCode() == 0 || pyRsp.getCode() == 200){
            schema = pyRsp.getData().toString();
        }

        try {
            AiModelEntity newEntity = new AiModelEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.desc);
            newEntity.setArea(req.area);
            newEntity.setPrice(req.price);
            newEntity.setVersion(req.version);
            newEntity.setAlgoId(req.algoId);
            newEntity.setTransform(dataTransform);
            newEntity.setSchema(schema);
            newEntity.setRunId(req.runId);
            newEntity.setTags(req.tags.toString());
            newEntity.setDeployTo(req.deployTo);
            newEntity.setEndpoint(req.endpoint);
            newEntity.setPubFlag(req.pubFlag);
            newEntity.setRate(req.rate);
            newEntity.setPubFlag(false);
            newEntity.setStatus(0);
            newEntity.setOrg(orgRepository.findById(tokenOrgId).get());
            //create_time and update_time are generated automatically by jp

            // save source
            modelRepository.save(newEntity);
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.area) || StrUtil.isEmpty(req.desc)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiModelEntity targetEntity = modelRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            if(!targetEntity.getDeployTo().equals(req.deployTo) || !targetEntity.getEndpoint().equals(req.endpoint)){
                // reset status when deployTo or endpoint is changed
                targetEntity.setStatus(0);
            }
            targetEntity.setName(req.name);
            targetEntity.setDesc(req.desc);
            targetEntity.setArea(req.area);
            targetEntity.setTags(req.tags.toString());
            targetEntity.setAlgoId(req.algoId);
            targetEntity.setRate(req.rate);
            targetEntity.setPrice(req.price);
            targetEntity.setDeployTo(req.deployTo);
            targetEntity.setEndpoint(req.endpoint);

            // create_time and update_time are generated automatically by jpa
            // runId, version, orgId should not be updated

            // update pre-trained model
            modelRepository.save(targetEntity);
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

        AiModelEntity targetEntity = modelRepository.findById(params.id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            // update public status
            targetEntity.setPubFlag(params.pub);
            modelRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/deploy")
    @Operation(description = "deployModel")
    public UniformResponse deployModel(@RequestBody @Parameter(name = "req", description = "model info") AiModelActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.area)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<AiModelEntity> duplicatedEntities = modelRepository.findByNameAndArea(req.name, req.area);
        if(duplicatedEntities.size()!=0){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        return UniformResponse.ok();
    }

    @DeleteMapping("/delete")
    @Operation(description = "deleteModel")
    public UniformResponse deleteModel(@RequestParam @Parameter(name = "id", description = "model id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");


        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiModelEntity targetEntity = modelRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        if(targetEntity.getStatus() != 0){
            return UniformResponse.error(UniformResponseCode.AI_MODEL_SERVING);
        }

        try {
            // delete entity
            modelRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }


    @PostMapping("/remove")
    @Operation(description = "removeModel")
    public UniformResponse removeModel(@RequestBody @Parameter(name = "id", description = "model id") AiModelRemoveReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");


        if(req.algoId==0 || req.version == 0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        AiModelEntity targetEntity = modelRepository.findByAlgoIdAndVersion(req.algoId, req.version).get(0);
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        if(targetEntity.getStatus() != 0){
            return UniformResponse.error(UniformResponseCode.AI_MODEL_SERVING);
        }

        try {
            // delete entity
            modelRepository.deleteById(targetEntity.getId());
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
        List<AiModelEntity> datasetEntities = modelRepository.findAll();

        // convert list to tree by category
        Map<String, List<AiModelEntity>> datasetMap = datasetEntities.stream().collect(Collectors.groupingBy(t -> t.getArea()));
        List<TreeSelect> treeDatasets = new ArrayList<>();
        Integer i = 1000;
        for(String group : datasetMap.keySet()){
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for(AiModelEntity source: datasetMap.get(group)){
                TreeSelect treeNode = new TreeSelect(source.getId(), source.getArea(), source.getName(), source.getName(), true, true);
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

        Set<Object> distinctType = modelRepository.findDistinctType();
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

        Set<Object> distinctCategory = modelRepository.findDistinctCategory();
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

    private JSONArray listRegisteredModels(String runIds) throws Exception {
        if(!dbUtils.isSourceExist(mLflowConfig.getId())){
            // add datasource to manage
            dbUtils.add(mLflowConfig.getId(), mLflowConfig.getName(), mLflowConfig.getType(), mLflowConfig.getUrl(),
                    mLflowConfig.getParams(), mLflowConfig.getUsername(), mLflowConfig.getPassword());
        }

        List<ColumnField> cols = new ArrayList<>();
        List<Object[]> result = new ArrayList<>();
        String sqlText = """
                with runs as (
                    select v.*, r.user_id as trained_by, r.start_time as trained_at, r.experiment_id FROM runs r
                    join (
                        select mv.name, mv.version, mv.run_id as run_uuid, mv.status, mv.user_id as registered_by, mv.last_updated_time as registered_at
                        from model_versions mv join model_version_tags mvt on mv.name=mvt.name AND mv.version=mvt.version
                        where
                            current_stage != 'Deleted_Internal'
                            AND mvt.key = 'user_id'
                            AND run_id IN (%s)
                    )v using(run_uuid)
                ),
                
                register as (
                    select run_uuid, group_concat(reg) as register from (
                        select r.run_uuid as run_uuid, concat_ws('":"', concat('"', mvt.key), concat(mvt.value, '"')) as reg from runs r join model_version_tags mvt  on r.name=mvt.name AND r.version=mvt.version
                    )g
                    group by run_uuid
                ),
                
                metrics as (
                    select run_uuid, group_concat(metric) as metrics 
                    from (
                        select r.run_uuid, concat_ws('":"', concat('"', m.key), concat(ROUND(m.value, 3), '"')) as metric from runs r join metrics m using(run_uuid) where m.value != 'None' and m.value is not null and locate('_unknown_', m.key)=0
                    )y
                    group by run_uuid
                ),
                
                in_schema as (
                    select run_uuid, dataset_schema as in_schema from runs r join datasets d using (experiment_id)
                )

                select r.*, concat('{', register, '}') as register, concat('{', metrics, '}') as metrics, in_schema
                from runs r 
                join register g using(run_uuid) 
                join metrics m using(run_uuid) 
                join in_schema d using(run_uuid) 
                """;

        sqlText = sqlText.formatted(runIds);
        // get query result
        dbUtils.execute(mLflowConfig.getId(), sqlText, cols, result);

        // build response
        JSONArray records = new JSONArray();
        for(int row = 0; row < result.size(); row++){
            Object[] objs = result.get(row);
            JSONObject jsonObject = new JSONObject();
            for(int m=0; m<cols.size(); m++){
                String key = cols.get(m).getName();
                if(objs[m] != null){
                    String val = objs[m].toString();
                    if(val.startsWith("{\"")){
                        // convert string to json object
                        JSONObject tmpValue = new JSONObject(val.replace("training_", ""));
                        if(key.equals("in_schema")){
                            jsonObject.set(key, tmpValue.get("mlflow_colspec"));
                        } else {
                            jsonObject.set(key, tmpValue);
                        }
                    } else if(key.equals("deployed")){
                        // convert string to bool
                        jsonObject.set(key, Boolean.valueOf(val));
                    } else {
                        jsonObject.set(key, val);
                    }
                }
            }
            // record response
            records.add(jsonObject);
        }

        return records;
    }
}
