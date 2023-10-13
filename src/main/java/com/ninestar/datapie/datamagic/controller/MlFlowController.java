package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.MlFlowEntity;
import com.ninestar.datapie.datamagic.repository.MlFlowEntityRepository;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.TreeUtils;
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
@RequestMapping("/workflow")
@Api(tags = "workflow")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MlFlowController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    public MlFlowEntityRepository workflowRepository;

    @PostMapping("/list")
    @ApiOperation(value = "getWorkflowList", httpMethod = "POST")
    public UniformResponse getWorkflowList(@RequestBody @ApiParam(name = "req", value = "request") TableListReqType req) throws IOException, InterruptedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");


        Long totalRecords = 0L;
        Page<MlFlowEntity> pageEntities = null;
        List<MlFlowEntity> queryEntities = null;

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
        Specification<MlFlowEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenUsername, req.filter, req.search);

        // query data from database
        if(pageable!=null){
            pageEntities = workflowRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = workflowRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<WorkflowListRspType> rspList = new ArrayList<WorkflowListRspType>();
        for(MlFlowEntity entity: queryEntities){
            WorkflowListRspType item = new WorkflowListRspType();
            BeanUtil.copyProperties(entity, item, new String[]{"graph","grid","config"});
            item.graph = new JSONObject(entity.getGraph()); // convert string to json array
            item.config = new JSONArray(entity.getConfig());
            item.grid = new JSONObject(entity.getGrid());
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
    @ApiOperation(value = "createWorkflow", httpMethod = "POST")
    public UniformResponse createWorkflow(@RequestBody @ApiParam(name = "req", value = "dataset info") WorkflowActionReqType req){
        //Hibernate: insert into sys_user (active, avatar, create_time, created_by, deleted, department, email, name, realname, org_id, password, phone, update_time, updated_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || req.graph==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<MlFlowEntity> duplicatedEntities = workflowRepository.findByNameAndGroup(req.name, req.category);
        if(duplicatedEntities!=null){
            for(MlFlowEntity entity: duplicatedEntities){
                if(entity.getId() == req.id){
                    return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
                }
            }
        }

        try {
            MlFlowEntity newEntity = new MlFlowEntity();
            //don't set ID for creating
            newEntity.setPid(0);
            newEntity.setName(req.name);
            newEntity.setDesc(req.description);
            newEntity.setGroup(req.category);
            newEntity.setGraph(req.graph.toString());
            newEntity.setGraphVer(req.graphVer);
            newEntity.setVersion("0");
            newEntity.setGrid(req.grid.toString());
            newEntity.setConfig(req.config.toString());
            newEntity.setLastRun(req.lastRun);
            newEntity.setDuration(req.duration);
            newEntity.setStatus(req.status);
            newEntity.setError(req.error);
            newEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jp

            // save new entity
            workflowRepository.save(newEntity);

            List<MlFlowEntity> targetEntities = workflowRepository.findByNameAndGroup(req.name, req.category);
            for(MlFlowEntity item: targetEntities){
                if(item==newEntity){
                    // the main entity has same pid and id
                    item.setPid(item.getId());
                    workflowRepository.save(item);
                    break;
                }
            }

            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @ApiOperation(value = "updateWorkflow", httpMethod = "POST")
    public UniformResponse updateWorkflow(@RequestBody @ApiParam(name = "req", value = "dataset info") WorkflowActionReqType req){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(req.id==0 || StrUtil.isEmpty(req.name) || req.graph==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlFlowEntity targetEntity = workflowRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setName(req.name);
            targetEntity.setDesc(req.description);
            targetEntity.setGroup(req.category);
            targetEntity.setGraphVer(req.graphVer);
            targetEntity.setGraph(req.graph.toString());
            targetEntity.setGrid(req.grid.toString());
            targetEntity.setConfig(req.config.toString());
            targetEntity.setLastRun(req.lastRun);
            targetEntity.setDuration(req.duration);
            targetEntity.setStatus(req.status);
            targetEntity.setError(req.error);
            targetEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jpa

            // update source
            workflowRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }


    @PostMapping("/branch")
    @ApiOperation(value = "branchWorkflow", httpMethod = "POST")
    public UniformResponse branchWorkflow(@RequestBody @ApiParam(name = "req", value = "dataset info") WorkflowActionReqType req){
        //Hibernate: insert into sys_user (active, avatar, create_time, created_by, deleted, department, email, name, realname, org_id, password, phone, update_time, updated_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(StrUtil.isEmpty(req.name) || req.graph==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        String newSubVersion = "0";
        List<MlFlowEntity> relatedEntities = workflowRepository.findByPidAndVersionStartingWith(req.pid, req.version);
        if(relatedEntities!=null){
            // find latest sub version
            Integer latestSubIdx = 0;
            for(MlFlowEntity entity: relatedEntities){
                if(entity.getVersion().length() == req.version.length() + 1){
                    String lastChar = entity.getVersion().substring(entity.getVersion().length()-1);
                    Integer temp = Integer.parseInt(lastChar);
                    if(temp>latestSubIdx){
                        latestSubIdx = temp;
                    }
                }
            }

            // valid sub version
            newSubVersion = req.version + latestSubIdx++;
        }

        try {
            MlFlowEntity newEntity = new MlFlowEntity();
            //don't set ID for creating
            newEntity.setPid(req.pid);
            newEntity.setName(req.name);
            newEntity.setDesc(req.description);
            newEntity.setGroup(req.category);
            newEntity.setGraph(req.graph.toString());
            newEntity.setGraphVer(req.graphVer);
            newEntity.setVersion(newSubVersion);
            newEntity.setGrid(req.grid.toString());
            newEntity.setConfig(req.config.toString());
            newEntity.setLastRun(req.lastRun);
            newEntity.setDuration(req.duration);
            newEntity.setStatus(req.status);
            newEntity.setError(req.error);
            newEntity.setPubFlag(req.pubFlag);
            //create_time and update_time are generated automatically by jp

            // save new entity
            workflowRepository.save(newEntity);

            List<MlFlowEntity> targetEntities = workflowRepository.findByNameAndGroup(req.name, req.category);
            for(MlFlowEntity item: targetEntities){
                if(item==newEntity){
                    // the main entity has same pid and id
                    item.setPid(item.getId());
                    workflowRepository.save(item);
                    break;
                }
            }

            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }



    @LogAnn(logType = LogType.ACTION, actionType = ActionType.SHARE)
    @PostMapping("/public")
    @ApiOperation(value = "publicWorkflow", httpMethod = "POST")
    public UniformResponse publicWorkflow(@RequestBody @ApiParam(name = "params", value = "dataset id and pub flag") PublicReqType params){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(params.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlFlowEntity targetEntity = workflowRepository.findById(params.id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            // update public status
            targetEntity.setPubFlag(params.pub);
            workflowRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.CLONE)
    @PostMapping("/clone")
    @ApiOperation(value = "cloneWorkflow", httpMethod = "POST")
    public UniformResponse cloneWorkflow(@RequestBody @ApiParam(name = "param", value = "dataset id") JSONObject param){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(param.get("id").toString());
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlFlowEntity targetEntity = workflowRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String copyName = targetEntity.getName();
        List<MlFlowEntity> targetCopies = workflowRepository.findByNameContainingOrderByIdDesc(copyName+"(");
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
            workflowRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @ApiOperation(value = "deleteWorkflow", httpMethod = "DELETE")
    public UniformResponse deleteWorkflow(@RequestParam @ApiParam(name = "id", value = "dataset id") Integer id){
        //Hibernate: delete from data_source where id=?
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlFlowEntity targetEntity = workflowRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            // delete entity
            workflowRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/tree")
    @ApiOperation(value = "getWorkflowTree", httpMethod = "POST")
    public UniformResponse getWorkflowTree(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        // jpa page is starting with 0
        List<MlFlowEntity> queryEntities = workflowRepository.findAll();

        if(queryEntities==null){
            return UniformResponse.ok();
        }

        try {
            //build tree list
            List<MlFlowEntity> treeWorkflows = TreeUtils.buildTree(queryEntities, "id", "pid", "children");
            return UniformResponse.ok().data(treeWorkflows);
        }catch (Exception e){
            return UniformResponse.error();
        }
    }

    @PostMapping("/getone")
    @ApiOperation(value = "getWorkflow", httpMethod = "POST")
    public UniformResponse getWorkflow(@RequestBody @ApiParam(name = "param", value = "dataset id") JSONObject param){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(param.get("id").toString());

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlFlowEntity targetEntity = workflowRepository.findById(id).get();
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

        Set<Object> distinctCategory = workflowRepository.findDistinctGroup();
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
