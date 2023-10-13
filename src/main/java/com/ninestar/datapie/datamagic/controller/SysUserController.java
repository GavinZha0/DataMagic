package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.SysOrgEntity;
import com.ninestar.datapie.datamagic.entity.SysRoleEntity;
import com.ninestar.datapie.datamagic.entity.SysUserEntity;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.repository.SysRoleRepository;
import com.ninestar.datapie.datamagic.repository.SysUserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
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
@RequestMapping("/user")
@Api(tags = "User")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class SysUserController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysUserRepository userRepository;

    @Resource
    private SysOrgRepository orgRepository;

    @Resource
    private SysRoleRepository roleRepository;

    @Resource
    private PasswordEncoder pswEncoder;

    @PostMapping("/list")
    @ApiOperation(value = "listUser", httpMethod = "POST")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    public UniformResponse listUser(@RequestBody @ApiParam(name = "request", value = "request") TableListReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Long totalRecords = 0L;
        Page<SysUserEntity> pageEntities = null;
        List<SysUserEntity> queryEntities = null;

        // put multiple orders into a sort which will be put into a pageable
        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort sortable = null;
        Pageable pageable = null;

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

        if(!tokenIsSuperuser){
            // regular admin can't see deleted users
            if(request.filter==null){
                request.filter = new TableListReqType.FilterType();
            }
            request.filter.fields.add("deleted");
            String[] tmp = {"false"};
            request.filter.values.add(tmp);
        }

        // build JPA specification
        Specification<SysUserEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenUsername, request.filter, request.search);

        // query data from database
        if(pageable!=null){
            pageEntities = userRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = userRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build api response
        for(SysUserEntity entity: queryEntities){
            // add roles to response
            List<String> roleList = new ArrayList<>();
            for(SysRoleEntity sysRole: entity.getRoles()){
                roleList.add(sysRole.getName());
            }
            entity.setRoleNames(roleList.toArray(new String[roleList.size()]));

            entity.setPassword("******"); // remove password
            entity.setOId(entity.getOrg().getId()); // org id
            entity.setOrgName(entity.getOrg().getName()); // org name
            entity.setOrg(null); // set entity to null
            entity.setRoles(null); // set entity to null
        }

        // build common response
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", queryEntities);
        jsonResponse.set("total", totalRecords);
        if(request.page!=null && request.page.current>0){
            jsonResponse.set("current", request.page.current);
        }
        else{
            jsonResponse.set("current", 1);
        }

        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/tree")
    @ApiOperation(value = "getUserTree", httpMethod = "POST")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    public UniformResponse getUserTree(@RequestBody @ApiParam(name = "request", value = "request") TableListReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        List<SysUserEntity> targetEntities = null;
        List<SysOrgEntity> orgEntities = orgRepository.findByPid(tokenOrgId);

        List<SysUserEntity> queryEntities = userRepository.findByOrgId(tokenOrgId);


        // build api response
        for(SysUserEntity entity: queryEntities){
            // add roles to response
            List<String> roleList = new ArrayList<>();
            for(SysRoleEntity sysRole: entity.getRoles()){
                roleList.add(sysRole.getName());
            }
            entity.setRoleNames(roleList.toArray(new String[roleList.size()]));

            entity.setPassword("******"); // remove password
            entity.setOId(entity.getOrg().getId()); // org id
            entity.setOrgName(entity.getOrg().getName()); // org name
            entity.setOrg(null); // set entity to null
            entity.setRoles(null); // set entity to null
        }


        return UniformResponse.ok().data(queryEntities);
    }


    @PostMapping("/one")
    @ApiOperation(value = "getOneUser", httpMethod = "POST")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    public UniformResponse getOneUser(@RequestBody @ApiParam(name = "request", value = "user id") JSONObject request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        // find user
        Integer userId = Integer.parseInt(request.get("id").toString());
        SysUserEntity entity = userRepository.findById(userId).get();

        if(!tokenIsSuperuser && !entity.getOrg().getId().equals(tokenOrgId)){
            // current user doesn't have permission to review this info
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        // add roles to response
        List<String> roleList = new ArrayList<>();
        for(SysRoleEntity sysRole: entity.getRoles()){
            roleList.add(sysRole.getName());
        }
        entity.setRoleNames(roleList.toArray(new String[roleList.size()]));

        entity.setPassword("******"); // remove password
        entity.setOId(entity.getOrg().getId()); // org id
        entity.setOrgName(entity.getOrg().getName()); // org name
        entity.setOrg(null); // set entity to null
        entity.setRoles(null); // set entity to null

        return UniformResponse.ok().data(entity);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "createUser", httpMethod = "POST")
    public UniformResponse createUser(@RequestBody @ApiParam(name = "user", value = "user info") UserActionReqType user){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(user.name) || StrUtil.isEmpty(user.password)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysUserEntity duplicatedUser = userRepository.findByNameAndOrgIdAndDeleted(user.name, tokenOrgId, false);
        if(duplicatedUser!=null){
            return UniformResponse.error(UniformResponseCode.USER_HAS_EXISTED);
        }

        try {
            SysUserEntity newEntity = new SysUserEntity();
            //don't set ID for create
            newEntity.setName(user.name);
            newEntity.setPassword(pswEncoder.encode(user.password));
            newEntity.setRealname(user.realname);
            newEntity.setAvatar(user.avatar);
            newEntity.setEmail(user.email);
            newEntity.setPhone(user.phone);
            newEntity.setSocial(user.social);
            newEntity.setActive(false);
            newEntity.setExpDate(user.expDate);
            newEntity.setDeleted(false);
            //created_by, create_time, updated_by and update_time are generated automatically by jpa (JpaAuditorAware)

            SysOrgEntity targetOrgEntity = orgRepository.findByName(user.orgName);

            // get and fill org
            if(tokenIsSuperuser){
                // superuser can create a user for any org
                newEntity.setOrg(targetOrgEntity);
            } else {
                Integer orgId = targetOrgEntity.getId();
                while(true){
                    if(orgId == tokenOrgId){
                        // regular user can create a user for same org only
                        newEntity.setOrg(targetOrgEntity);
                        break;
                    } else {
                        orgId = orgRepository.findById(orgId).get().getPid();
                    }
                }
            }

            // find and add roles to new entity
            for(String role: user.roleNames){
                SysRoleEntity roleEntity = roleRepository.findByName(role);
                newEntity.getRoles().add(roleEntity);
            }

            // save user and roles
            userRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @ApiOperation(value = "updateUser", httpMethod = "POST")
    public UniformResponse updateUser(@RequestBody @ApiParam(name = "user", value = "user info") UserActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(req.id==0 || StrUtil.isEmpty(req.name)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysUserEntity targetEntity = userRepository.findByName(req.name);
        if(targetEntity==null || targetEntity.getDeleted()){
            // deleted user can't be updated
            return UniformResponse.error(UniformResponseCode.USER_NOT_EXIST);
        }

        try {
            // no password is updated here
            if(req.part==null || req.part == 0){
                // base info
                targetEntity.setRealname(req.realname);
                targetEntity.setDesc(req.desc);
            }
            if(req.part==null || req.part == 1){
                // contact info
                targetEntity.setEmail(req.email);
                targetEntity.setPhone(req.phone);
                targetEntity.setSocial(req.social);
            }
            if(req.part==null){
                targetEntity.setAvatar(req.avatar);
                targetEntity.setActive(req.active);
                targetEntity.setExpDate(req.expDate);

                SysOrgEntity targetOrgEntity = orgRepository.findByName(req.orgName);
                // get and fill org
                if(tokenIsSuperuser){
                    // superuser can create a user for any org
                    targetEntity.setOrg(targetOrgEntity);
                } else {
                    Integer orgId = targetOrgEntity.getId();
                    while(true){
                        if(orgId == tokenOrgId){
                            // regular user can create a user for same org only
                            targetEntity.setOrg(targetOrgEntity);
                            break;
                        } else {
                            orgId = orgRepository.findById(orgId).get().getPid();
                        }
                    }
                }

                // find and add roles to target entity
                targetEntity.getRoles().clear();
                for(String role: req.roleNames){
                    SysRoleEntity roleEntity = roleRepository.findByName(role);
                    targetEntity.getRoles().add(roleEntity);
                }
            }

            // update user and roles
            userRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/password")
    @ApiOperation(value = "updatePassword", httpMethod = "POST")
    public UniformResponse updatePassword(@RequestBody @ApiParam(name = "password", value = "password") UserPasswordReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());

        if(req.id==0 || StrUtil.isEmpty(req.oldPwd) || StrUtil.isEmpty(req.newPwd)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysUserEntity targetEntity = userRepository.findById(req.id).get();
        if(targetEntity==null || targetEntity.getDeleted()){
            return UniformResponse.error(UniformResponseCode.USER_NOT_EXIST);
        }

        try {
            if(req.id == tokenUserId && pswEncoder.matches(req.oldPwd, targetEntity.getPassword())){
                // only yourself can update your password
                targetEntity.setPassword(pswEncoder.encode(req.newPwd));
            }

            // update
            userRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ACTIVE)
    @PostMapping("/active")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "activateUser", httpMethod = "POST")
    public UniformResponse activateUser(@RequestBody @ApiParam(name = "request", value = "user id and active") ActiveReqType request){
        if(request.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysUserEntity targetEntity = userRepository.findById(request.id).get();
        if(targetEntity==null || targetEntity.getDeleted()){
            return UniformResponse.error(UniformResponseCode.USER_NOT_EXIST);
        }

        try {
            // update active and save
            targetEntity.setActive(request.active);
            userRepository.save(targetEntity);
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
    @ApiOperation(value = "deleteUser", httpMethod = "DELETE")
    public UniformResponse deleteUser(@RequestParam @ApiParam(name = "id", value = "user id") Integer id){
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysUserEntity targetEntity = userRepository.findById(id).get();

        try {
            // delete entity (set deleted to true)
            targetEntity.setActive(false);
            targetEntity.setDeleted(true);
            userRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/options")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "getUserOptions", httpMethod = "POST")
    public UniformResponse getUserOptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        List<SysUserEntity> sysUsers = userRepository.findByOrgIdOrOrgIdIsNull(tokenOrgId);

        List<OptionsRspType> userList = new ArrayList<OptionsRspType>();
        for(SysUserEntity sysUser: sysUsers){
            if(sysUser.getActive()){
                OptionsRspType user = new OptionsRspType();
                BeanUtil.copyProperties(sysUser, user);
                userList.add(user);
            }
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", userList);
        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/upload")
    @ApiOperation(value = "AvatarUpload", httpMethod = "POST")
    public UniformResponse AvatarUpload(@RequestParam("file") MultipartFile file) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();

        if(file==null){
            return UniformResponse.error();
        }

        // file server is under project temporarily
        // later it should be replaced by a real file server like minio or cloud storage solution ??? Gavin
        String ftpDir = System.getProperty("user.dir") + "/fileServer";
        //ftpDir = uploadPath; // path config
        String fileDir = ftpDir + "/" + tokenOrgId + "/avatar/";

        // create destination folder
        File targetFolder = new File(fileDir);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        //build file name based on username and image type
        String filename = file.getContentType();
        filename = filename.replace("/", ".");
        filename = filename.replace("image", tokenUsername);

        try {
            file.transferTo(new File(fileDir + filename));
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
            return UniformResponse.error(e.getMessage());
        }

        // update user avatar
        SysUserEntity targetEntity = userRepository.findById(tokenUserId).get();
        targetEntity.setAvatar(fileDir + filename);
        userRepository.save(targetEntity);
        return UniformResponse.ok();
    }
}
