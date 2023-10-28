package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.SysMenuEntity;
import com.ninestar.datapie.datamagic.entity.SysRoleEntity;
import com.ninestar.datapie.datamagic.entity.SysUserEntity;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.repository.SysRoleRepository;
import com.ninestar.datapie.datamagic.repository.SysUserRepository;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.TreeNode;
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
import java.util.*;
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
@RequestMapping("/role")
@Api(tags = "Role")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SysRoleController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysRoleRepository roleRepository;

    @Resource
    private SysOrgRepository orgRepository;

    @Resource
    private SysUserRepository userRepository;


    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "getRoleList", httpMethod = "POST")
    public UniformResponse getRoleList(@RequestBody @ApiParam(name = "req", value = "request") TableListReqType req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Long totalRecords = 0L;
        Page<SysRoleEntity> pageEntities = null;
        List<SysRoleEntity> queryEntities = null;

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
        Specification<SysRoleEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenIsAdmin, tokenUsername, req.filter, req.search);

        // query data from database
        if(pageable!=null){
            pageEntities = roleRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = roleRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<RoleListRspType> rspList = new ArrayList<RoleListRspType>();
        for(SysRoleEntity entity: queryEntities){
            RoleListRspType item = new RoleListRspType();
            BeanUtil.copyProperties(entity, item);
            //shared role if org is null
            item.orgName = entity.getOrg()==null?null:entity.getOrg().getName();
            item.orgId = entity.getOrg()==null?null:entity.getOrg().getId();
            item.userCount = entity.getUsers().size();
            for(SysUserEntity user: entity.getUsers()){
                item.userIds.add(user.getId());
            }
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
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "createRole", httpMethod = "POST")
    public UniformResponse createRole(@RequestBody @ApiParam(name = "user", value = "role info") RoleActionReqType role){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(r->r.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(role.name)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysRoleEntity duplicatedEntity = roleRepository.findByNameAndOrgId(role.name, role.orgId);
        if(duplicatedEntity!=null){
            return UniformResponse.error(UniformResponseCode.USER_HAS_EXISTED);
        }

        try {
            SysRoleEntity newEntity = new SysRoleEntity();
            //don't set ID for create
            newEntity.setName(role.name);
            newEntity.setDesc(role.desc);
            newEntity.setActive(false);
            // get and fill org
            newEntity.setOrg(orgRepository.findById(role.orgId).get());

            // save role
            roleRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "updateRole", httpMethod = "POST")
    public UniformResponse updateRole(@RequestBody @ApiParam(name = "role", value = "role info") RoleActionReqType role){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(r->r.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(role.id==0 || StrUtil.isEmpty(role.name)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysRoleEntity targetEntity = roleRepository.findById(role.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.USER_NOT_EXIST);
        }

        try {
            targetEntity.setName(role.name);
            targetEntity.setDesc(role.desc);
            targetEntity.setActive(role.active);

            // get and fill org
            if(tokenIsSuperuser && role.orgId != null){
                // superuser can update a user to any org
                targetEntity.setOrg(orgRepository.findById(role.orgId).get());
            } else {
                // regular user can update a user for same org only
                targetEntity.setOrg(orgRepository.findById(tokenOrgId).get());
            }

            // update user and roles
            roleRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.GRANT)
    @PostMapping("/permit")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "geRolePermit", httpMethod = "POST")
    public UniformResponse geRolePermit(@RequestBody @ApiParam(name = "request", value = "role id") JSONObject request) {
        Integer id = Integer.parseInt(request.get("id").toString());
        Set<SysMenuEntity> rolePermits = roleRepository.findById(id).get().getPermits();
        List<AuthPermitRspType> permitList = new ArrayList<AuthPermitRspType>();
        for(SysMenuEntity rolePermit: rolePermits){
            AuthPermitRspType permit = new AuthPermitRspType();
            BeanUtil.copyProperties(rolePermit, permit);
            permitList.add(permit);
        }
        return UniformResponse.ok().data(permitList);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ACTIVE)
    @PostMapping("/active")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "activeRole", httpMethod = "POST")
    public UniformResponse activeRole(@RequestBody @ApiParam(name = "request", value = "role id") ActiveReqType request){
        if(request.id==0){
            return UniformResponse.error(UniformResponseCode.API_EXCEPTION);
        }

        SysRoleEntity targetEntity = roleRepository.findById(request.id).get();
        if(targetEntity==null){
            //target role doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            // update active status
            targetEntity.setActive(request.active);
            roleRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "deleteRole", httpMethod = "DELETE")
    public UniformResponse deleteRole(@RequestParam @ApiParam(name = "id", value = "role id") Integer id){
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysRoleEntity targetEntity = roleRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        try {
            // delete entity
            roleRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/user_list")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "getRoleUsers", httpMethod = "POST")
    public UniformResponse getRoleUsers(@RequestBody @ApiParam(name = "request", value = "role id") JSONObject request) {
        Integer roleId = Integer.parseInt(request.get("id").toString());

        SysRoleEntity entity = roleRepository.findById(roleId).get();
        List<SysUserEntity> queryEntities = new ArrayList<SysUserEntity>();
        // get all users which has specific role
        queryEntities.addAll(entity.getUsers());

        if(queryEntities==null){
            return UniformResponse.ok();
        }

        try {
            List<TreeNode> rspTree = new ArrayList<>();
            // group users by organization and department
            Map<String, List<SysUserEntity>> userMap = queryEntities.stream().collect(Collectors.groupingBy(t -> t.getOrg().getName()));

            // convert map to tree
            int i = 1000;
            for(Map.Entry<String, List<SysUserEntity>> org: userMap.entrySet()){
                TreeNode orgTree = new TreeNode(i, org.getKey(), org.getKey(), false, false);
                i++;
                rspTree.add(orgTree);
            }

            return UniformResponse.ok().data(rspTree);
        }catch (Exception e){
            return UniformResponse.error();
        }
    }


    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/user_update")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "updateRoleUsers", httpMethod = "POST")
    public UniformResponse updateRoleUsers(@RequestBody @ApiParam(name = "req", value = "role user list") RoleActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(r->r.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(req.id==null || req.id == 0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysRoleEntity targetEntity = roleRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.USER_NOT_EXIST);
        }

        try {
            targetEntity.getUsers().clear();
            if(req.userIds.size()>0){
                Set<SysUserEntity> setUsers = new HashSet<>();
                for(Integer userId: req.userIds){
                    setUsers.add(userRepository.findById(userId).get());

                }
                targetEntity.setUsers(setUsers);
            }

            // update user and roles
            roleRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }


    @PostMapping("/options")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "getRoleOptions", httpMethod = "POST")
    public UniformResponse getRoleOptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        List<SysRoleEntity> sysRoles = roleRepository.findByOrgIdOrOrgIdIsNull(tokenOrgId);

        List<OptionsRspType> roleList = new ArrayList<OptionsRspType>();
        for(SysRoleEntity sysRole: sysRoles){
            if(sysRole.getActive()){
                OptionsRspType role = new OptionsRspType();
                BeanUtil.copyProperties(sysRole, role);
                roleList.add(role);
            }
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", roleList);
        return UniformResponse.ok().data(jsonResponse);
    }
}
