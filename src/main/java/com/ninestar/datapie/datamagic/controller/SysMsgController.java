package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.ActiveReqType;
import com.ninestar.datapie.datamagic.bridge.MsgActionReqType;
import com.ninestar.datapie.datamagic.entity.SysMsgEntity;
import com.ninestar.datapie.datamagic.repository.SysMsgRepository;
import com.ninestar.datapie.datamagic.repository.SysUserRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
@RequestMapping("/msg")
@Api(tags = "Msg")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SysMsgController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysMsgRepository msgRepository;

    @Resource
    private SysUserRepository userRepository;

    @PostMapping("/list")
    @ApiOperation(value = "getMsgList", httpMethod = "POST")
    public UniformResponse getMsgList(@RequestBody @ApiParam(name = "req", value = "msg info") MsgActionReqType req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Integer targetUserId = tokenUserId;

        if(tokenIsSuperuser && req.id!=null){
            targetUserId = req.id;
        }


        List<SysMsgEntity> targetEntities = null;
        if(req.inbox){
            targetEntities = msgRepository.findByUserIdOrOrgIdOrderByTsUtcDesc(targetUserId, tokenOrgId);
        } else {
            targetEntities = msgRepository.findByFromIdOrderByTsUtcDesc(tokenUserId);
        }

        for(SysMsgEntity msgEntity: targetEntities){
            msgEntity.setFrom(msgEntity.getFromUser().getRealname());
            if(msgEntity.getType().equalsIgnoreCase("MESSAGE")){
                msgEntity.setTo(msgEntity.getToUser().getRealname());
            } else {
                msgEntity.setTo(msgEntity.getToOrg().getName());
            }

            // remove sub entities for response
            msgEntity.setFromUser(null);
            msgEntity.setToUser(null);
            msgEntity.setToOrg(null);
        }

        return UniformResponse.ok().data(targetEntities);
    }

    @PostMapping("/get")
    @ApiOperation(value = "getUserMsg", httpMethod = "POST")
    public UniformResponse getUserMsg(@RequestBody @ApiParam(name = "request", value = "user id") JSONObject request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Boolean tokenSuperuser = auth.getAuthorities().contains("ROLE_superuser");
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        Integer id = Integer.parseInt(request.get("id").toString());
        Long totalRecords = 0L;
        Page<SysMsgEntity> pageEntities = null;
        List<SysMsgEntity> queryEntities = null;


        // build sort object
        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort.Order order = new Sort.Order(Sort.Direction.ASC, "ts");
        orders.add(order);
        Sort sortable = Sort.by(orders);

        // build page object with/without sort
        Pageable pageable = null;
        if(true){
            // jpa page starts from 0
            if(orders.size()>0){
                pageable = PageRequest.of(0, 20, sortable);
            }
            else{
                pageable = PageRequest.of(0, 20);
            }
        }

        // query data from database
        List<SysMsgEntity> allEntities = msgRepository.findAll();


        return UniformResponse.ok().data(allEntities);
    }


    @PostMapping("/tree")
    @ApiOperation(value = "getMsgTree", httpMethod = "POST")
    public UniformResponse getMsgTree(@RequestBody @ApiParam(name = "request", value = "menu name") JSONObject request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUser = auth.getCredentials().toString();
        Boolean tokenSuperuser = auth.getAuthorities().contains("ROLE_superuser");
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        // get all first
        List<SysMsgEntity> queryEntities = msgRepository.findAll();

        if(queryEntities==null){
            return UniformResponse.ok();
        }

        try {
            //build tree list
            List<SysMsgEntity> treeMsgs = TreeUtils.buildTree(queryEntities, "id", "pid", "children");
            SysMsgEntity targetTree = null;
            for(SysMsgEntity msg: treeMsgs) {
                if (msg.getId().equals(request.get("name").toString())) {
                    // find target
                    targetTree = msg;
                }
            }

            return UniformResponse.ok().data(targetTree);
        }catch (Exception e){
            return UniformResponse.error();
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @ApiOperation(value = "createMsg", httpMethod = "POST")
    public UniformResponse createMsg(@RequestBody @ApiParam(name = "request", value = "msg info") SysMsgEntity req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(StrUtil.isEmpty(req.getMsg())){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        try {
            req.setType("MESSAGE");
            req.setFromUser(userRepository.findById(tokenUserId).get());
            req.setToUser(userRepository.findByRealname(req.getTo()));
            req.setRead(false);

            // save source
            msgRepository.save(req);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ACTIVE)
    @PostMapping("/read")
    @ApiOperation(value = "readMsg", httpMethod = "POST")
    public UniformResponse readMsg(@RequestBody @ApiParam(name = "request", value = "msg read") ActiveReqType req){
        if(req.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        Optional<SysMsgEntity> targetEntity = msgRepository.findById(req.id);
        if(targetEntity.isEmpty()){
            return UniformResponse.error();
        }

        try {
            // update active and save
            targetEntity.get().setRead(req.active);
            msgRepository.save(targetEntity.get());
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @ApiOperation(value = "deleteMsg", httpMethod = "DELETE")
    public UniformResponse deleteMsg(@RequestParam @ApiParam(name = "id", value = "msg id") Integer id){
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        try {
            msgRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

}
