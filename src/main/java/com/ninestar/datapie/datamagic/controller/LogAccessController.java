package com.ninestar.datapie.datamagic.controller;

import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.LogAccessEntity;
import com.ninestar.datapie.datamagic.repository.*;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/logaccess")
@Api(tags = "LogAccess")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class LogAccessController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private LogAccessRepository accRepository;

    @PostMapping("/list")
    @ApiOperation(value = "listAccLog", httpMethod = "POST")
    @PreAuthorize("hasAnyRole('Superuser')")
    public UniformResponse listAccLog(@RequestBody @ApiParam(name = "request", value = "request") TableListReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        Long totalRecords = 0L;
        Page<LogAccessEntity> pageEntities = null;
        List<LogAccessEntity> queryEntities = null;

        // put multiple orders into a sort which will be put into a pageable
        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort sortable = null;
        Pageable pageable = null;
/*
        if(request.sorter==null){
            // default sort by ts
            request.sorter = new TableListReqType.SorterType();
            request.sorter.fields = new String[1];
            request.sorter.fields[0] = "ts";
            request.sorter.orders = new String[1];
            request.sorter.orders[0] = "descent";
        }
*/
        // build sort object
        //support multiple orders
        if(request.sorter!=null && request.sorter.orders!=null && request.sorter.orders.length>0){
            for(int i=0; i< request.sorter.fields.length; i++){
                Sort.Order order = null;
                if(request.sorter.orders[i].equalsIgnoreCase("ascend")){
                    order = new Sort.Order(Sort.Direction.ASC, request.sorter.fields[i]);
                }
                else{
                    order = new Sort.Order(Sort.Direction.DESC, request.sorter.fields[i]);
                }
                orders.add(order);
            }
            sortable = Sort.by(orders);
        }

        // build page object with/without sort
        if(request.page!=null){
            // jpa page starts from 0
            if(request.sorter!=null && request.sorter.fields != null && request.sorter.fields.length>0){
                pageable = PageRequest.of(request.page.current-1, request.page.pageSize, sortable);
            }
            else{
                pageable = PageRequest.of(request.page.current-1, request.page.pageSize);
            }
        }

        // build JPA specification
        Specification<LogAccessEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenUsername,  request.filter, request.search);

        // query data from database
        if(pageable!=null){
            pageEntities = accRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = accRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build common response
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("records", queryEntities);
        jsonResponse.put("total", totalRecords);
        if(request.page!=null && request.page.current>0){
            jsonResponse.put("current", request.page.current);
        }
        else{
            jsonResponse.put("current", 1);
        }


        return UniformResponse.ok().data(jsonResponse);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @ApiOperation(value = "deleteAccLog", httpMethod = "DELETE")
    @PreAuthorize("hasAnyRole('Superuser')")
    public UniformResponse deleteUser(@RequestParam @ApiParam(name = "id", value = "user id") Integer id){
        //set deleted to true
        //Hibernate: update sys_user set active=?, avatar=?, create_time=?, created_by=?, deleted=?, department=?, email=?, name=?, realname=?, org_id=?, password=?, phone=?, update_time=?, updated_by=? where id=?

        //delete really
        //Hibernate: delete from sys_user_role where user_id=?
        //Hibernate: delete from sys_user where id=?
        //Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        //Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        LogAccessEntity targetEntity = accRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            accRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

}
