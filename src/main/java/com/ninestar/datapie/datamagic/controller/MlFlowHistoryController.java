package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.WorkflowActionReqType;
import com.ninestar.datapie.datamagic.entity.MlFlowEntity;
import com.ninestar.datapie.datamagic.entity.MlFlowHistoryEntity;
import com.ninestar.datapie.datamagic.repository.*;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/ml/flow/history")
@Api(tags = "MlFlowHistory")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MlFlowHistoryController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String pyServerUrl = "http://localhost:9538/ml/";

    @Resource
    public MlFlowHistoryRepository flowHistoryRepository;

    @Resource
    public MlFlowRepository workflowRepository;

    @Resource
    public SysOrgRepository orgRepository;

    @PostMapping("/list")
    @ApiOperation(value = "getFlowHistoryList", httpMethod = "POST")
    public UniformResponse getFlowHistoryList(@RequestBody @ApiParam(name = "param", value = "param") JSONObject param) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Boolean succOnly = Boolean.parseBoolean(param.get("succOnly").toString());
        Integer flowId = Integer.parseInt(param.get("flowId").toString());
        List<MlFlowHistoryEntity> algoHis = new ArrayList<>();
        if(succOnly){
            algoHis = flowHistoryRepository.findByFlowIdAndCreatedByAndStatusOrderByCreatedAtDesc(flowId, tokenUsername, 0);
        } else {
            algoHis = flowHistoryRepository.findByFlowIdAndCreatedByOrderByCreatedAtDesc(flowId, tokenUsername);
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", algoHis);
        return UniformResponse.ok().data(jsonResponse);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @ApiOperation(value = "createFlowHistory", httpMethod = "POST")
    public UniformResponse createFlowHistory(@RequestBody @ApiParam(name = "req", value = "dataset info") WorkflowActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(req.name) || req.workflow==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<MlFlowEntity> duplicatedEntities = workflowRepository.findByNameAndGroup(req.name, req.group);
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
            newEntity.setName(req.name);
            newEntity.setDesc(req.desc);
            newEntity.setGroup(req.group);
            newEntity.setWorkflow(req.workflow.toString());
            newEntity.setX6Ver(req.x6Ver);
            newEntity.setVersion("0");
            newEntity.setCanvas(req.canvas.toString());
            newEntity.setConfig(req.config.toString());
            newEntity.setPubFlag(false);
            newEntity.setOrg(orgRepository.findById(tokenOrgId).get());
            //create_time and update_time are generated automatically by jp

            // save new entity
            workflowRepository.save(newEntity);

            List<MlFlowEntity> targetEntities = workflowRepository.findByNameAndGroup(req.name, req.group);
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
    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @ApiOperation(value = "deleteFlowHistory", httpMethod = "DELETE")
    public UniformResponse deleteFlowHistory(@RequestParam @ApiParam(name = "id", value = "history id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");


        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlFlowHistoryEntity targetEntity = flowHistoryRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            // delete entity
            flowHistoryRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/getone")
    @ApiOperation(value = "getFlowHistory", httpMethod = "POST")
    public UniformResponse getFlowHistory(@RequestBody @ApiParam(name = "param", value = "history id") JSONObject param){
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

        MlFlowHistoryEntity targetEntity = flowHistoryRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        return UniformResponse.ok().data(targetEntity);
    }
}
