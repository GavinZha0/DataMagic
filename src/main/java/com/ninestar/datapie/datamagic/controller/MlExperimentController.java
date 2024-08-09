package com.ninestar.datapie.datamagic.controller;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.annotation.SingleReqParam;
import com.ninestar.datapie.datamagic.config.MLflowConfig;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.utils.DbUtils;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.ColumnField;
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

        List<ColumnField> cols = new ArrayList<>();
        List<Object[]> result = new ArrayList<>();
        String experName = "algo_"+ id;
        String sqlText = """
                with runs as(
                  select run_uuid, status, start_time, round((end_time-start_time)/60000,1) as duration from experiments m join runs r using(experiment_id) where m.name = '%s'
                ),
                params as (
                  select run_uuid, group_concat(param) as params from (
                    select r.*, concat_ws('":"', concat('"', p.key), concat(p.value, '"')) as param  from runs r join params p using(run_uuid) where p.value != 'None' and p.value is not null
                  )x
                  group by run_uuid
                ),
                metrics as (
                  select run_uuid, group_concat(eval) as evals from (
                    select r.run_uuid, concat_ws('":"', concat('"', m.key), concat(ROUND(m.value, 3), '"')) as eval from runs r join metrics m using(run_uuid) where m.value != 'None' and m.value is not null and locate('_unknown_', m.key)=0
                  )y
                  group by run_uuid
                )
                select * from (
                  select start_time, run_uuid, status, duration, concat('{', params, '}') as params, concat('{', evals, '}') as evals from runs r join params p using(run_uuid) join metrics m using(run_uuid)    
                )z
                order by start_time DESC
                """;
        sqlText = sqlText.formatted(experName);
        try {
            // get query result
            dbUtils.execute(mLflowConfig.getId(), sqlText, cols, result);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return UniformResponse.error(UniformResponseCode.API_EXCEPTION_SQL_EXE);
        }

        JSONArray records = new JSONArray();
        JSONObject retObj = new JSONObject();
        for(int row = 0; row < result.size(); row++){
            Object[] objs = result.get(row);
            JSONObject jsonObject = new JSONObject();
            for(int m=0; m<cols.size(); m++){
                String val = objs[m].toString();
                if(val.startsWith("{\"")){
                    // convert string to json object
                    jsonObject.set(cols.get(m).getName(), new JSONObject(val));
                } else {
                    jsonObject.set(cols.get(m).getName(), val);
                }
            }
            records.add(jsonObject);
            // use start_time as key
            retObj.set(objs[0].toString(), jsonObject);
        }

        return UniformResponse.ok().data(retObj);
    }
}
