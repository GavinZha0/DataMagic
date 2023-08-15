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
import com.ninestar.datapie.datamagic.entity.MlModelEntity;
import com.ninestar.datapie.datamagic.service.AsyncTaskService;
import com.ninestar.datapie.datamagic.repository.MlModelRepository;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.TreeSelect;
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

import javax.annotation.Resource;
import javax.script.*;
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
@RequestMapping("/model")
@Api(tags = "Model")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MlModelController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String FILE_SERVER = System.getProperty("user.dir") + "/fileServer";

    private static final String pyServerUrl = "http://localhost:9538/ml/";

    @Resource
    private AsyncTaskService asyncService;

    @Resource
    public MlModelRepository modelRepository;

    @PostMapping("/list")
    @ApiOperation(value = "getModelList", httpMethod = "POST")
    public UniformResponse getModelList(@RequestBody @ApiParam(name = "req", value = "request") TableListReqType req) throws IOException, InterruptedException, ScriptException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Boolean tokenSuperuser = auth.getAuthorities().contains("ROLE_superuser");
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());


        /*
        // run python in java

        Properties props = new Properties();
        props.put("python.home", "/Users/jzhao1/anaconda3/bin/");
        props.put("python.console.encoding", "UTF-8");
        props.put("python.security.respectJavaAccessibility", "false");
        props.put("python.import.site", "false");
        Properties preprops = System.getProperties();
        PythonInterpreter.initialize(preprops, props, new String[0]);
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("print('.......start')");
        //Jpython can't import other lib
        //ImportError: No module named numpy
        //interpreter.exec("import numpy");
        interpreter.exec("print('........end')");


        StringWriter writer = new StringWriter();
        ScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setWriter(writer);
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("python");
        engine.eval("print('........engine start')");
        //ImportError: No module named numpy
        //engine.eval("import numpy");
        //engine.eval(new FileReader("/Users/jzhao1/Documents/ML_Proj/sklearn-clf/clf_2features_2classes.py"), scriptContext);
        engine.eval("print('........engine end')");

*/

/*
        JSONObject param = new JSONObject();
        param.set("first", 7.34);
        param.set("second", 4.65);

        String pythonServerUrl = "http://127.0.0.1:9538/predict";
        HttpResponse rsp = HttpRequest.post(pythonServerUrl)
                .body(param.toString())
                .execute();
        JSONObject result = new JSONObject(rsp.body());
*/


        Long totalRecords = 0L;
        Page<MlModelEntity> pageEntities = null;
        List<MlModelEntity> queryEntities = null;

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
        Specification<MlModelEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenSuperuser,req.filter, req.search);

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
        List<MlModelListRspType> rspList = new ArrayList<MlModelListRspType>();
        for(MlModelEntity entity: queryEntities){
            MlModelListRspType item = new MlModelListRspType();
            BeanUtil.copyProperties(entity, item, new String[]{"config"});
            item.config = new JSONObject(entity.getConfig());
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
    @ApiOperation(value = "createModel", httpMethod = "POST")
    public UniformResponse createModel(@RequestBody @ApiParam(name = "req", value = "dataset info") MlModelActionReqType req){
        //Hibernate: insert into sys_user (active, avatar, create_time, created_by, deleted, department, email, name, realname, org_id, password, phone, update_time, updated_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.framework) || StrUtil.isEmpty(req.frameVer) || StrUtil.isEmpty(req.content)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<MlModelEntity> duplicatedEntities = modelRepository.findByNameAndGroup(req.name, req.category);
        if(duplicatedEntities!=null){
            for(MlModelEntity entity: duplicatedEntities){
                if(entity.getPid() == req.id){
                    return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
                }
            }
        }

        try {
            MlModelEntity newEntity = new MlModelEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.description);
            newEntity.setGroup(req.category);
            newEntity.setPid(0);
            newEntity.setType(req.type);
            newEntity.setFramework(req.framework);
            newEntity.setFrameVer(req.frameVer);
            newEntity.setContent(req.content);
            if(req.config!=null){
                newEntity.setConfig(req.config.toString());
            }
            newEntity.setPubFlag(req.pubFlag);
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

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @ApiOperation(value = "updateModel", httpMethod = "POST")
    public UniformResponse updateModel(@RequestBody @ApiParam(name = "req", value = "Model info") MlModelActionReqType req){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.framework) || StrUtil.isEmpty(req.frameVer) || StrUtil.isEmpty(req.content)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlModelEntity targetEntity = modelRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setId(req.id);
            targetEntity.setName(req.name);
            targetEntity.setType(req.type);
            targetEntity.setDesc(req.description);
            targetEntity.setGroup(req.category);
            targetEntity.setPid(req.pid);
            targetEntity.setFramework(req.framework);
            targetEntity.setFrameVer(req.frameVer);
            targetEntity.setConfig(req.config.toString());
            targetEntity.setContent(req.content);
            targetEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jpa

            // update source
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
    @ApiOperation(value = "publicModel", httpMethod = "POST")
    public UniformResponse publicModel(@RequestBody @ApiParam(name = "params", value = "dataset id and pub flag") PublicReqType params){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(params.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlModelEntity targetEntity = modelRepository.findById(params.id).get();
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

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.CLONE)
    @PostMapping("/clone")
    @ApiOperation(value = "cloneALgorithm", httpMethod = "POST")
    public UniformResponse cloneALgorithm(@RequestBody @ApiParam(name = "param", value = "dataset id") JSONObject param){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(param.get("id").toString());
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlModelEntity targetEntity = modelRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String copyName = targetEntity.getName();
        List<MlModelEntity> targetCopies = modelRepository.findByNameContainingOrderByIdDesc(copyName+"(");
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
            modelRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @ApiOperation(value = "deleteModel", httpMethod = "DELETE")
    public UniformResponse deleteModel(@RequestParam @ApiParam(name = "id", value = "dataset id") Integer id){
        //Hibernate: delete from data_source where id=?
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlModelEntity targetEntity = modelRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
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

    @PostMapping("/execute")
    @ApiOperation(value = "execute", httpMethod = "POST")
    public UniformResponse execute(@RequestBody @ApiParam(name = "param", value = "algorithm id") JSONObject param) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //String loginUser = auth.getCredentials().toString();
        String orgId = auth.getDetails().toString();
        String  userId = auth.getPrincipal().toString();


        // model id
        Integer id = Integer.parseInt(param.get("id").toString());

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlModelEntity targetEntity = modelRepository.findById(id).get();
        if(targetEntity==null || targetEntity.getContent()==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }


        //Java版的交互式编程工具 -- Jshell。Jshell是从JDK9开始支持的，可以执行单个语句，也可以导入已有的java文件。

        Timestamp ts = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String formatedTS = sdf.format(ts);
        String outputDir = FILE_SERVER + "/" + orgId + "/" + userId + "/ML_model/" + id + "/" + formatedTS;
        String msgTarget = userId + "_model" + id;




        // run DL4J/DJL example
        CompletableFuture<Integer> future = null;
        if(targetEntity.getFramework().equalsIgnoreCase("DL4J")){
            logger.info("Start a thread to execute DL4J model, inform UI to start progressbar via stomp......");
            future = asyncService.executeDl4j(outputDir, msgTarget);
            // callback after the task is finished
            future.thenAccept((ret)->{
                logger.info("DL4J thread is over with return {}, inform UI to stop progressbar via stomp......", ret);
            });
            // non-blocking return
            return UniformResponse.ok();
        }
        else if(targetEntity.getFramework().equalsIgnoreCase("DJL")){
            logger.info("Start a thread to execute DJL model, inform UI to start progressbar via stomp......");
            future = asyncService.executeDJL(outputDir, msgTarget);
            // callback after the task is finished
            future.thenAccept((ret)->{
                logger.info("DJL thread is over with return {}, inform UI to stop progressbar via stomp......", ret);
            });
            // non-blocking return
            return UniformResponse.ok();
        }

        // control plane
        // forward command to python server for user x and algorithm y
        HttpResponse response = null;
        try{
            String uniqueId = userId.toString() + "_model"+targetEntity.getId();
            response = HttpRequest.post(pyServerUrl + "execute")
                    .header("uid", uniqueId)
                    .body(JSONUtil.parseObj(targetEntity).toString()) // need to do toJsonString. Gavin!!!
                    .execute();
        }catch (Exception e){
            logger.error(e.getMessage());
            return UniformResponse.error(e.getMessage());
        }

        if(response!=null){
            // forward response of python server to front end
            JSONObject result = new JSONObject(response.body());
            return result.toBean(UniformResponse.class);
        }
        else{
            return UniformResponse.error();
        }
    }

    @PostMapping("/execute_script")
    @ApiOperation(value = "executeScript", httpMethod = "POST")
    public UniformResponse executeScript(@RequestBody @ApiParam(name = "request", value = "request info") ModelActionReqType request) throws Exception {
        //Hibernate: update sys_user set active=?, avatar=?, create_time=?, created_by=?, deleted=?, department=?, email=?, name=?, realname=?, org_id=?, password=?, phone=?, update_time=?, updated_by=? where id=?
        //Hibernate: delete from sys_user_role where user_id=?
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //String loginUser = auth.getCredentials().toString();
        String orgId = auth.getDetails().toString();
        String  userId = auth.getPrincipal().toString();

        if(request==null || request.id==null || StrUtil.isEmpty(request.content) || StrUtil.isEmpty(request.language)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        String pythonServerUrl = "http://localhost:9538/ml/execute_script";
        HttpResponse response = HttpRequest.post(pythonServerUrl)
                .body(JSONUtil.parseObj(request).toString()) // need to do toJsonString. Gavin!!!
                .execute();
        JSONObject result = new JSONObject(response.body());

        return result.toBean(UniformResponse.class);
        //return UniformResponse.ok().data(result);
    }

    @PostMapping("/tree")
    @ApiOperation(value = "getModelTree", httpMethod = "POST")
    public UniformResponse getModelTree(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        // jpa page is starting with 0
        List<MlModelEntity> datasetEntities = modelRepository.findAll();

        // convert list to tree by category
        Map<String, List<MlModelEntity>> datasetMap = datasetEntities.stream().collect(Collectors.groupingBy(t -> t.getGroup()));
        List<TreeSelect> treeDatasets = new ArrayList<>();
        Integer i = 1000;
        for(String group : datasetMap.keySet()){
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for(MlModelEntity source: datasetMap.get(group)){
                TreeSelect treeNode = new TreeSelect(source.getId(), source.getType(), source.getName(), source.getName(), true, true);
                treeGroup.getChildren().add(treeNode);
            }
            treeDatasets.add(treeGroup);
            i+=100;
        }

        return UniformResponse.ok().data(treeDatasets);
    }

    @PostMapping("/getone")
    @ApiOperation(value = "getModel", httpMethod = "POST")
    public UniformResponse getModel(@RequestBody @ApiParam(name = "param", value = "dataset id") JSONObject param){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(param.get("id").toString());

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlModelEntity targetEntity = modelRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        DatasetListRspType response = new DatasetListRspType();
        BeanUtil.copyProperties(targetEntity, response);
        return UniformResponse.ok().data(response);
    }

    @PostMapping("/category")
    @ApiOperation(value = "getCatOptions", httpMethod = "POST")
    public UniformResponse getCatOptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        Set<Object> distinctCategory = modelRepository.findDistinctGroup();
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
}
