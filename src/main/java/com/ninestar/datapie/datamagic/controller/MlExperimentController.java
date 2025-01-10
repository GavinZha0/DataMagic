package com.ninestar.datapie.datamagic.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.annotation.SingleReqParam;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.AuthLoginRspType;
import com.ninestar.datapie.datamagic.config.MLflowConfig;
import com.ninestar.datapie.datamagic.entity.MlAlgoEntity;
import com.ninestar.datapie.datamagic.entity.MlExperimentEntity;
import com.ninestar.datapie.datamagic.repository.MlAlgoRepository;
import com.ninestar.datapie.datamagic.repository.MlExperimentRepository;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.utils.DbUtils;
import com.ninestar.datapie.datamagic.utils.JwtTokenUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.ColumnField;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.mlflow.tracking.MlflowClient;
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
@RequestMapping("/ml/experiment")
@Tag(name = "MlExperiment")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class MlExperimentController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private DbUtils dbUtils;

    @Resource
    private MLflowConfig mLflowConfig;

    @Resource
    public SysOrgRepository orgRepository;

    @Resource
    public MlExperimentRepository experimentRepository;

    @Resource
    private MlAlgoRepository algoRepository;


    @PostMapping("/list")
    @Operation(description = "getMlExperimentList")
    public UniformResponse getMlExperimentList(@RequestBody @Parameter(name = "param", description = "param") JSONObject param) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Boolean succOnly = Boolean.parseBoolean(param.get("succOnly").toString());
        Integer algoId = Integer.parseInt(param.get("algoId").toString());
        List<MlExperimentEntity> experList = new ArrayList<>();
        experList = experimentRepository.findByMlIdAndUserIdOrderByStartAtDesc(algoId, tokenUserId);

        MlflowClient client = new MlflowClient("// admin:admin#520@datapie. cjiaoci4g12w. us-east-1.rds. amazonaws. com:3306/ mlflow");

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", experList);
        return UniformResponse.ok().data(jsonResponse);
    }


    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @Operation(description = "createMlExperiment")
    public UniformResponse createMlExperiment(@SingleReqParam @Parameter(name = "id", description = "algo id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        MlAlgoEntity algoEntity = algoRepository.findById(id).get();

        // build request parameters
        JSONObject pyParams = new JSONObject();
        pyParams.set("id", id);

        // create a new token
        AuthLoginRspType userInfo = new AuthLoginRspType();
        userInfo.id = tokenUserId;
        userInfo.name = tokenUsername;
        userInfo.orgId = tokenOrgId;
        String token = JwtTokenUtil.createToken(userInfo, null);

        // send http request to python server
        HttpResponse response = HttpRequest.post("http://localhost:9138/ml/algo/execute")
                .header("authorization", "Bearer " + token)
                .body(pyParams.toString())
                .execute();

        // decode response of python server
        JSONObject result = new JSONObject(response.body());
        UniformResponse pyRsp = result.toBean(UniformResponse.class);
        if(pyRsp.getCode() != 0 && pyRsp.getCode() != 200){
            return pyRsp;
        }

        try {
            MlExperimentEntity newEntity = new MlExperimentEntity();
            //don't set ID for creating
            newEntity.setMlId(algoEntity.getId());
            newEntity.setName(algoEntity.getName());
            newEntity.setDesc(algoEntity.getDesc());
            newEntity.setType("algo");
            newEntity.setAlgo(algoEntity.getDataCfg());
            newEntity.setDataset(algoEntity.getDataCfg());
            newEntity.setTrain(algoEntity.getTrainCfg());
            newEntity.setTrials(null);
            newEntity.setStatus(0);
            newEntity.setUserId(tokenUserId);
            newEntity.setOrgId(tokenOrgId);
            //start_at and end_time are generated automatically by jp

            // save new entity
            experimentRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }
    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @Operation(description = "deleteExperiment")
    public UniformResponse deleteExperiment(@RequestParam @Parameter(name = "id", description = "experiment id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        MlExperimentEntity targetEntity = experimentRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            // delete entity
            experimentRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/trials")
    @Operation(description = "getTrials")
    public UniformResponse getTrials(@SingleReqParam @Parameter(name = "id", description = "algo id") Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(!dbUtils.isSourceExist(mLflowConfig.getId())){
            // add datasource to manage
            dbUtils.add(mLflowConfig.getId(), mLflowConfig.getName(), mLflowConfig.getType(), mLflowConfig.getUrl(),
                    mLflowConfig.getParams(), mLflowConfig.getUsername(), mLflowConfig.getPassword());
        }


        // MlflowClient support tracking server uri and databricks only
        // Create a Mlflow client and get/set something via the client API
        // String trackingUri = "http://localhost:5138";
        // MlflowClient client = new MlflowClient(trackingUri);
        // Service.Experiment exper = client.getExperiment("49");
        // logger.info(exper.getName());

        List<ColumnField> cols = new ArrayList<>();
        List<Object[]> result = new ArrayList<>();
        String experName = "algo_"+ id;
        // experiments.name = 'algo_{ID}_{TIME}'
        // get metrics of max(step) using 'having 1'
        String sqlText = """
                with runs as(
                  select m.experiment_id as exper_id, t.value as args, run_uuid, status, start_time as ts, round((end_time-start_time)/60000,1) as duration from experiments m join experiment_tags t using(experiment_id) join runs r using(experiment_id) where m.name like '%s\\_%%' and t.key='args' and r.user_id = %d
                ),
                params as (
                  select run_uuid, group_concat(param) as params from (
                    select r.*, concat_ws('":"', concat('"', p.key), concat(p.value, '"')) as param  from runs r join params p using(run_uuid) where p.value != 'None' and p.value is not null
                  )x
                  group by run_uuid
                ),
                metrics as (
                  select run_uuid, group_concat(metric) as metrics from (
                    select r.run_uuid, concat_ws('":"', concat('"', m.key), concat(ROUND(m.value, 3), '"')) as metric from runs r 
                    join (
                        select * from (select * from metrics s where s.value != 'None' and s.value is not null and locate('_unknown_', s.key)=0 having 1 order by step desc) k group by k.run_uuid, k.key
                    ) m using(run_uuid)
                  )y
                  group by run_uuid
                ),
                reg as (
                  select v.run_id as run_uuid, v.version, t.key, t.value as published from model_versions v join model_version_tags t on v.name=t.name AND v.version=t.version where current_stage != 'Deleted_Internal' AND t.key='published'
                )
                select z.*, g.version, g.published from (
                  select r.*, concat('{', params, '}') as params, concat('{', metrics, '}') as metrics from runs r join params p using(run_uuid) join metrics m using(run_uuid)
                )z
                left join reg g using(run_uuid)
                order by ts DESC
                """;
        sqlText = sqlText.formatted(experName, tokenUserId);
        try {
            // get query result
            dbUtils.execute(mLflowConfig.getId(), sqlText, cols, result);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return UniformResponse.error(UniformResponseCode.API_EXCEPTION_SQL_EXE);
        }

        // build response
        JSONArray records = new JSONArray();
        for(int row = 0; row < result.size(); row++){
            Object[] objs = result.get(row);
            JSONObject jsonObject = new JSONObject();
            for(int m=0; m<cols.size(); m++){
                String key = cols.get(m).getName();
                if(objs[m] != null){
                    String val = objs[m].toString();
                    if(key.equals("args")){
                        // args='aaa|bbb|ccc'
                        // convert args to array
                        jsonObject.set(key, val.split("\\|"));
                    } else if(val.startsWith("{\"")){
                        // convert string to json object
                        jsonObject.set(key, new JSONObject(val.replace("training_", "")));
                    } else if(key.equals("published")){
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

        return UniformResponse.ok().data(records);
    }
}
