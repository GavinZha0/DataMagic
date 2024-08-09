package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.annotation.SingleReqParam;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.MsgActionReqType;
import com.ninestar.datapie.datamagic.entity.SysMsgEntity;
import com.ninestar.datapie.datamagic.entity.SysOrgEntity;
import com.ninestar.datapie.datamagic.entity.SysUserEntity;
import com.ninestar.datapie.datamagic.repository.SysMsgRepository;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.repository.SysUserRepository;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.TreeUtils;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "SysMsg")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SysMsgController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysMsgRepository msgRepository;

    @Resource
    private SysUserRepository userRepository;

    @Resource
    private SysOrgRepository orgRepository;

    @PostMapping("/list")
    @Operation(summary = "getMsgList")
    public UniformResponse getMsgList(@RequestBody @Parameter(name = "req") MsgActionReqType req) {
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
            if(msgEntity.getFromId() != null){
                // from a user
                SysUserEntity user = userRepository.findById(msgEntity.getFromId()).get();
                msgEntity.setFrom(user.getRealname());
            }

            if(msgEntity.getType().equals("notice")){
                // to an org
                SysOrgEntity org = orgRepository.findById(msgEntity.getToId()).get();
                msgEntity.setTo(org.getName());
            } else {
                // to a user
                SysUserEntity user = userRepository.findById(msgEntity.getToId()).get();
                msgEntity.setTo(user.getRealname());
            }
        }

        return UniformResponse.ok().data(targetEntities);
    }

    @PostMapping("/get_unread_msg")
    @Operation(summary = "getUnreadMsg")
    public UniformResponse getUnreadMsg() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");


        // query data from database
        List<SysMsgEntity> notice = msgRepository.findUnreadNoticeById(tokenOrgId, tokenUserId, tokenUserId, tokenUserId, tokenUserId);
        for(SysMsgEntity item: notice){
            if(item.getFromId() != null){
                SysUserEntity user = userRepository.findById(item.getFromId()).get();
                item.setFrom(user.getRealname());
            }
        }
        List<SysMsgEntity> message = msgRepository.findUnreadMsgById(tokenUserId);
        for(SysMsgEntity item: message){
            if(item.getFromId() != null) {
                SysUserEntity user = userRepository.findById(item.getFromId()).get();
                item.setFrom(user.getRealname());
            }
        }
        JSONObject rsp = new JSONObject();
        rsp.set("notice", notice);
        rsp.set("msg", message);
        return UniformResponse.ok().data(rsp);
    }


    @PostMapping("/get")
    @Operation(summary = "getUserMsg")
    public UniformResponse getUserMsg(@RequestBody @Parameter(description = "user id") JSONObject request) {
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
    @Operation(summary = "getMsgTree")
    public UniformResponse getMsgTree(@RequestBody @Parameter(description = "menu name") JSONObject request) {
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
    @Operation(summary = "createMsg")
    public UniformResponse createMsg(@RequestBody @Parameter(description = "msg info") SysMsgEntity req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(StrUtil.isEmpty(req.getContent())){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        try {
            req.setType("msg");
            req.setReadUsers("[]");

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
    @Operation(summary = "readMsg")
    public UniformResponse readMsg(@SingleReqParam @Parameter(description = "msg id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        // find the msg
        SysMsgEntity targetEntity = msgRepository.findById(id).get();
        try {
            // update active and save
            List<String> readUsers = JSONUtil.parseArray(targetEntity.getReadUsers()).toList(String.class);
            if (!readUsers.contains(String.valueOf(tokenUserId))){
                // add user to read list
                readUsers.add(String.valueOf(tokenUserId));
                JSONArray jsonArray = new JSONArray(readUsers);
                targetEntity.setReadUsers(jsonArray.toString());
                msgRepository.save(targetEntity);
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
    @Operation(summary = "deleteMsg")
    public UniformResponse deleteMsg(@RequestParam @Parameter(name = "id", description = "msg id") Integer id){
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
