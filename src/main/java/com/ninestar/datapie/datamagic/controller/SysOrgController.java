package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.util.StrUtil;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.*;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.repository.SysUserRepository;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.TreeUtils;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/org")
@Api(tags = "Org")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SysOrgController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysOrgRepository orgRepository;

    @Resource
    private SysUserRepository userRepository;


    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "getOrgList", httpMethod = "POST")
    public UniformResponse getOrgList() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort.Order order = new Sort.Order(Sort.Direction.ASC, "name");
        orders.add(order);
        Sort sortable = Sort.by(orders);

        List<SysOrgEntity> queryEntities = null;
        if(tokenIsSuperuser){
            // get all orgs for superuser
            queryEntities = orgRepository.findAll(sortable);
        } else {
            // get all existing orgs for regular admin
            queryEntities = orgRepository.findByDeleted(false, sortable);
        }

        if(queryEntities==null){
            return UniformResponse.ok();
        }

        for(SysOrgEntity entity: queryEntities){
            // get total users of every org
            entity.setUserCount(userRepository.findByOrgId(entity.getId()).size());
        }

        try {
            //build tree list based on id and pid
            List<SysOrgEntity> treeOrgs = TreeUtils.buildTree(queryEntities, "id", "pid", "children");
            if(tokenIsSuperuser) {
                // superuser can see all org trees
                return UniformResponse.ok().data(treeOrgs);
            } else {
                // regular admin can see one complete org tree
                SysOrgEntity rootOrg = orgRepository.findById(tokenOrgId).get();
                while (rootOrg.getPid()!=null){
                    // find root org
                    rootOrg = orgRepository.findById(rootOrg.getPid()).get();
                }

                for(SysOrgEntity subOrgs: treeOrgs){
                    // find root tree of current admin
                    if(subOrgs.getId() == rootOrg.getId()){
                        // build list if there is only one tree to respond 'data.records'
                        List<SysOrgEntity> listOrg = new ArrayList<>();
                        listOrg.add(subOrgs);
                        return UniformResponse.ok().data(listOrg);
                    }
                }
            }
        }catch (Exception e){
            return UniformResponse.error();
        }
        return UniformResponse.ok();
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "createOrg", httpMethod = "POST")
    public UniformResponse createOrg(@RequestBody @ApiParam(name = "request", value = "config info") OrgActionReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(request.name)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysOrgEntity duplicatedEntity = orgRepository.findByName(request.name);
        if(duplicatedEntity!=null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        try {
            SysOrgEntity newEntity = new SysOrgEntity();
            newEntity.setName(request.name);
            newEntity.setDesc(request.desc);
            newEntity.setLogo(request.logo);
            newEntity.setExpDate(request.expDate);
            newEntity.setActive(false);
            newEntity.setDeleted(false);

            Boolean isValid = false;
            if(tokenIsSuperuser){
                newEntity.setPid(request.pid);
                isValid = true;
            } else {
                // admin can create sub org only
                Integer pOrgId = orgRepository.findById(request.pid).get().getId();
                while (pOrgId != null){
                    if(pOrgId == tokenOrgId){
                        newEntity.setPid(request.pid);
                        isValid = true;
                        break;
                    } else {
                        pOrgId = orgRepository.findById(pOrgId).get().getPid();
                    }

                }
            }

            if(isValid){
                // save org
                orgRepository.save(newEntity);
                return UniformResponse.ok();
            } else {
                return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
            }

        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "updateOrg", httpMethod = "POST")
    public UniformResponse updateConfig(@RequestBody @ApiParam(name = "source", value = "source info") OrgActionReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(request.id==0 || StrUtil.isEmpty(request.name)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysOrgEntity targetEntity = orgRepository.findByName(request.name);
        if(targetEntity==null || targetEntity.getDeleted()){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setName(request.name);
            targetEntity.setActive(request.active);
            targetEntity.setDesc(request.desc);
            targetEntity.setLogo(request.logo);
            targetEntity.setExpDate(request.expDate);
            //create_time and update_time are generated automatically by jpa

            Boolean isValid = false;
            if(tokenIsSuperuser){
                targetEntity.setPid(request.pid);
                isValid = true;
            } else {
                // admin can create sub org tree only
                Integer pOrg = orgRepository.findById(request.pid).get().getId();
                while (pOrg != null){
                    if(pOrg == tokenOrgId){
                        targetEntity.setPid(request.pid);
                        isValid = true;
                        break;
                    } else {
                        pOrg = orgRepository.findById(pOrg).get().getPid();
                    }
                }
            }

            if(isValid){
                // update source
                orgRepository.save(targetEntity);
                return UniformResponse.ok();
            } else {
                return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
            }

        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ACTIVE)
    @PostMapping("/active")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @ApiOperation(value = "activateOrg", httpMethod = "POST")
    public UniformResponse activateUser(@RequestBody @ApiParam(name = "request", value = "org id and active") ActiveReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(request.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysOrgEntity targetEntity = orgRepository.findById(request.id).get();
        if(targetEntity==null){
            //target user doesn't exist
            return UniformResponse.error(UniformResponseCode.USER_NOT_EXIST);
        }

        Boolean isValid = false;
        if(tokenIsSuperuser){
            isValid = true;
        } else {
            Integer pOrg = request.id;
            while (pOrg != null){
                if(pOrg == tokenOrgId){
                    // in same org tree
                    isValid = true;
                    break;
                } else {
                    pOrg = orgRepository.findById(pOrg).get().getPid();
                }
            }
        }

        if(!isValid){
            // no permit
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // update active and save
            targetEntity.setActive(request.active);
            orgRepository.save(targetEntity);
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
    @ApiOperation(value = "deleteOrg", httpMethod = "DELETE")
    public UniformResponse deleteUser(@RequestParam @ApiParam(name = "id", value = "org id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysOrgEntity targetEntity = orgRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        Boolean isValid = false;
        if(tokenIsSuperuser){
            isValid = true;
        } else {
            Integer pOrg = id;
            while (pOrg != null){
                if(pOrg == tokenOrgId){
                    // in same org tree
                    isValid = true;
                    break;
                } else {
                    pOrg = orgRepository.findById(pOrg).get().getPid();
                }
            }
        }

        if(!isValid){
            // no permit
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // delete entity (set deleted to true)
            targetEntity.setActive(false);
            targetEntity.setDeleted(true);
            orgRepository.save(targetEntity);
            //orgRepository.deleteById(id); // delete it really
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/options")
    @ApiOperation(value = "getOrgOptions", httpMethod = "POST")
    public UniformResponse getOrgOptions() throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        List<SysOrgEntity> treeOrgs = new ArrayList<>();
        if(tokenIsSuperuser){
            // get all active root orgs
            List<SysOrgEntity> rootOrgs = orgRepository.findByPidAndActive(null, true);
            for(SysOrgEntity org: rootOrgs){
                // build org tree
                org.setChildren(buildTree(org.getId()));
                treeOrgs.add(org);
            }
        } else {
            // one org tree
            SysOrgEntity rootOrg = orgRepository.findById(tokenOrgId).get();
            rootOrg.setChildren(buildTree(tokenOrgId));
            treeOrgs.add(rootOrg);
        }
        return UniformResponse.ok().data(treeOrgs);
    }

    private List<SysOrgEntity> buildTree(Integer id){
        List<SysOrgEntity> orgs = orgRepository.findByPid(id);
        for(SysOrgEntity org: orgs){
            org.setChildren(buildTree(org.getId()));
        }

        return orgs;
    }

}
