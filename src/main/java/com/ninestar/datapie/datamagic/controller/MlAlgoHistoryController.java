package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.MlAlgoEntity;
import com.ninestar.datapie.datamagic.entity.MlAlgoHistoryEntity;
import com.ninestar.datapie.datamagic.repository.MlAlgoHistoryRepository;
import com.ninestar.datapie.datamagic.repository.MlAlgoRepository;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;


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
@RequestMapping("/ml/algo/history")
@Tag(name = "MlAlgoHistory")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MlAlgoHistoryController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String pyServerUrl = "http://localhost:9538/ml/";

    @Resource
    public MlAlgoHistoryRepository algoHistoryRepository;

    @Resource
    public MlAlgoRepository algorithmRepository;

    @Resource
    public SysOrgRepository orgRepository;

    @PostMapping("/list")
    @Operation(description = "getAlgoHistoryList")
    public UniformResponse getAlgoHistoryList(@RequestBody @Parameter(name = "param", description = "param") JSONObject param) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Boolean succOnly = Boolean.parseBoolean(param.get("succOnly").toString());
        Integer algoId = Integer.parseInt(param.get("algoId").toString());
        List<MlAlgoHistoryEntity> algoHis = new ArrayList<>();
        if(succOnly){
            algoHis = algoHistoryRepository.findByAlgoIdAndCreatedByAndStatusOrderByCreatedAtDesc(algoId, tokenUsername, 0);
        } else {
            algoHis = algoHistoryRepository.findByAlgoIdAndCreatedByOrderByCreatedAtDesc(algoId, tokenUsername);
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", algoHis);
        return UniformResponse.ok().data(jsonResponse);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @Operation(description = "createAlgoHistory")
    public UniformResponse createAlgoHistory(@RequestBody @Parameter(name = "req", description = "history info") AlgorithmActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(req.name) || StrUtil.isEmpty(req.framework) || StrUtil.isEmpty(req.srcCode)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<MlAlgoEntity> duplicatedEntities = algorithmRepository.findByNameAndGroup(req.name, req.group);

        try {
            MlAlgoEntity newEntity = new MlAlgoEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.desc);
            newEntity.setGroup(req.group);
            newEntity.setCategory(req.category);
            newEntity.setFramework(req.framework);
            newEntity.setFrameVer("3.10");
            newEntity.setSrcCode(req.srcCode);
            if(req.dataCfg!=null){
                newEntity.setDataCfg(req.dataCfg.toString());
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

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @Operation(description = "deleteAlgoHistory")
    public UniformResponse deleteAlgoHistory(@RequestParam @Parameter(name = "id", description = "history id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");


        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlAlgoHistoryEntity targetEntity = algoHistoryRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            // delete entity
            algoHistoryRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/getone")
    @Operation(description = "getAlgoHistory")
    public UniformResponse getAlgoHistory(@RequestBody @Parameter(name = "param", description = "history id") JSONObject param){
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

        MlAlgoHistoryEntity targetEntity = algoHistoryRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        return UniformResponse.ok().data(targetEntity);
    }
}
