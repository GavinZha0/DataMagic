package com.ninestar.datapie.datamagic.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.MlAlgoEntity;
import com.ninestar.datapie.datamagic.repository.MlAlgoRepository;
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
@RequestMapping("/algorithm")
@Api(tags = "Algorithm")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MlAlgoController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String pyServerUrl = "http://localhost:9538/ml/";

    @Resource
    public MlAlgoRepository algorithmRepository;

    @PostMapping("/list")
    @ApiOperation(value = "getAlgorithmList", httpMethod = "POST")
    public UniformResponse getAlgorithmList(@RequestBody @ApiParam(name = "req", value = "request") TableListReqType req) throws InterruptedException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //String loginUser = auth.getCredentials().toString();
        String orgId = auth.getDetails().toString();
        String  userId = auth.getPrincipal().toString();
        Boolean tokenSuperuser = auth.getAuthorities().contains("ROLE_superuser");
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

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
        Specification<MlAlgoEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenSuperuser,req.filter, req.search);

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
    @ApiOperation(value = "createAlgorithm", httpMethod = "POST")
    public UniformResponse createAlgorithm(@RequestBody @ApiParam(name = "req", value = "dataset info") AlgorithmActionReqType req){
        //Hibernate: insert into sys_user (active, avatar, create_time, created_by, deleted, department, email, name, realname, org_id, password, phone, update_time, updated_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.language) || StrUtil.isEmpty(req.langVer) || StrUtil.isEmpty(req.content)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<MlAlgoEntity> duplicatedEntities = algorithmRepository.findByNameAndGroup(req.name, req.group);
        if(duplicatedEntities!=null){
            for(MlAlgoEntity entity: duplicatedEntities){
                if(entity.getPid() == req.id){
                    return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
                }
            }
        }

        try {
            MlAlgoEntity newEntity = new MlAlgoEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.desc);
            newEntity.setGroup(req.group);
            newEntity.setPid(0);
            newEntity.setType(req.type);
            newEntity.setLanguage(req.language);
            newEntity.setLangVer(req.langVer);
            newEntity.setContent(req.content);
            if(req.config!=null){
                newEntity.setConfig(req.config.toString());
            }
            newEntity.setPubFlag(req.pubFlag);
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
    @ApiOperation(value = "updateAlgorithm", httpMethod = "POST")
    public UniformResponse updateAlgorithm(@RequestBody @ApiParam(name = "req", value = "Algorithm info") AlgorithmActionReqType req){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.language) || StrUtil.isEmpty(req.langVer) || StrUtil.isEmpty(req.content)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlAlgoEntity targetEntity = algorithmRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setId(req.id);
            targetEntity.setName(req.name);
            targetEntity.setType(req.type);
            targetEntity.setDesc(req.desc);
            targetEntity.setGroup(req.group);
            targetEntity.setPid(req.pid);
            targetEntity.setLanguage(req.language);
            targetEntity.setLangVer(req.langVer);
            targetEntity.setConfig(req.config.toString());
            targetEntity.setContent(req.content);
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
    @ApiOperation(value = "publicAlgorithm", httpMethod = "POST")
    public UniformResponse publicAlgorithm(@RequestBody @ApiParam(name = "params", value = "dataset id and pub flag") PublicReqType params){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

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
    @ApiOperation(value = "cloneALgorithm", httpMethod = "POST")
    public UniformResponse cloneALgorithm(@RequestBody @ApiParam(name = "param", value = "dataset id") JSONObject param){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

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
    @ApiOperation(value = "deleteAlgorithm", httpMethod = "DELETE")
    public UniformResponse deleteAlgorithm(@RequestParam @ApiParam(name = "id", value = "dataset id") Integer id){
        //Hibernate: delete from data_source where id=?
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

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
    @ApiOperation(value = "getAlgorithmTree", httpMethod = "POST")
    public UniformResponse getAlgorithmTree(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        // jpa page is starting with 0
        List<MlAlgoEntity> datasetEntities = algorithmRepository.findAll();

        // convert list to tree by category
        Map<String, List<MlAlgoEntity>> datasetMap = datasetEntities.stream().collect(Collectors.groupingBy(t -> t.getGroup()));
        List<TreeSelect> treeDatasets = new ArrayList<>();
        Integer i = 1000;
        for(String group : datasetMap.keySet()){
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for(MlAlgoEntity source: datasetMap.get(group)){
                TreeSelect treeNode = new TreeSelect(source.getId(), source.getType(), source.getName(), source.getName(), true, true);
                treeGroup.getChildren().add(treeNode);
            }
            treeDatasets.add(treeGroup);
            i+=100;
        }

        return UniformResponse.ok().data(treeDatasets);
    }

    @PostMapping("/getone")
    @ApiOperation(value = "getAlgorithm", httpMethod = "POST")
    public UniformResponse getAlgorithm(@RequestBody @ApiParam(name = "param", value = "dataset id") JSONObject param){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

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
    @ApiOperation(value = "execute", httpMethod = "POST")
    public UniformResponse execute(@RequestBody @ApiParam(name = "param", value = "algorithm id") JSONObject param) throws Exception {
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();
        Integer userId = (Integer)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Integer id = Integer.parseInt(param.get("id").toString());

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlAlgoEntity targetEntity = algorithmRepository.findById(id).get();
        if(targetEntity==null || targetEntity.getContent()==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        // control plane
        // forward command to python server for user x and algorithm y
        HttpResponse response = null;
        try{
            String uniqueId = userId.toString() + "_alg"+targetEntity.getId();
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
    public UniformResponse executeScript(@RequestBody @ApiParam(name = "request", value = "request info") AlgorithmActionReqType request) throws Exception {
        //Hibernate: update sys_user set active=?, avatar=?, create_time=?, created_by=?, deleted=?, department=?, email=?, name=?, realname=?, org_id=?, password=?, phone=?, update_time=?, updated_by=? where id=?
        //Hibernate: delete from sys_user_role where user_id=?
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)

        //Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        //Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        if(request==null || request.id==null || StrUtil.isEmpty(request.content) || StrUtil.isEmpty(request.language)){
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






    @PostMapping("/category")
    @ApiOperation(value = "getCatOptions", httpMethod = "POST")
    public UniformResponse getCatOptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        Set<Object> distinctCategory = algorithmRepository.findDistinctGroup();
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
