package com.ninestar.datapie.datamagic.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.SysMenuEntity;
import com.ninestar.datapie.datamagic.entity.VizReportEntity;
import com.ninestar.datapie.datamagic.repository.SysMenuRepository;
import com.ninestar.datapie.datamagic.repository.VizDatareportRepository;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.TreeUtils;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
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
 * @since 2021-11-13
 */
@RestController
@RequestMapping("/menu")
@Tag(name = "SysMenu")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SysMenuController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysMenuRepository menuRepository;

    @Resource
    public VizDatareportRepository reportRepository;

    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @Operation(summary="getMenuList")
    public UniformResponse getMenuList() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort.Order order = new Sort.Order(Sort.Direction.ASC, "pos");
        orders.add(order);
        Sort sortable = Sort.by(orders);

        // get all first
        List<SysMenuEntity> queryEntities = menuRepository.findByDeleted(false, sortable);

        if(queryEntities==null){
            return UniformResponse.ok();
        }

        try {
            //build tree list
            List<SysMenuEntity> treeMenus = TreeUtils.buildTree(queryEntities, "id", "pid", "children");
            // clear up menu in reports in order to avoid exception
            SetMenuNull(treeMenus);

            if(tokenIsSuperuser) {
                // superuser can see all org tree
                return UniformResponse.ok().data(treeMenus);
            } else {
                // admin can see dashboard tree
                for(SysMenuEntity subMenus: treeMenus){
                    // find root tree of current admin
                    if(subMenus.getName().equalsIgnoreCase("DASHBOARD")){
                        List<SysMenuEntity> listMenu = new ArrayList<>();
                        listMenu.add(subMenus);
                        return UniformResponse.ok().data(listMenu);
                    }
                }
            }

        }catch (Exception e){
            return UniformResponse.error();
        }

        return UniformResponse.ok();
    }

    @PostMapping("/tree")
    //@PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @Operation(summary = "getMenuTree")
    public UniformResponse getMenuTree(@RequestBody @Validated JSONObject request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        List<Sort.Order> orders = new ArrayList<Sort.Order>();
        Sort.Order order = new Sort.Order(Sort.Direction.ASC, "pos");
        orders.add(order);
        Sort sortable = Sort.by(orders);

        // get all first
        List<SysMenuEntity> queryEntities = menuRepository.findByOrgIdActiveAndDeleted(tokenOrgId, true, false);

        if(queryEntities==null){
            return UniformResponse.ok();
        }

        try {
            //build tree list
            List<SysMenuEntity> treeMenus = TreeUtils.buildTree(queryEntities, "id", "pid", "children");
            // clear up menu in reports in order to avoid exception
            SetMenuNull(treeMenus);

            // 'publish' means to include both 'home' and 'dashboard'
            String reqName = request.get("name").toString();
            String[] targetNames = {"dashboard", "placeholder"};
            if(reqName.equalsIgnoreCase("publish")){
                targetNames[1] = "home";
            }

            // find target tree based on required name
            List<SysMenuEntity> targetTree = new ArrayList<>();
            for(SysMenuEntity menu: treeMenus) {
                if (menu.getName().equalsIgnoreCase(targetNames[0]) || menu.getName().equalsIgnoreCase(targetNames[1])) {
                    // find target
                    targetTree.add(menu);
                }
            }

            return UniformResponse.ok().data(targetTree);
        }catch (Exception e){
            return UniformResponse.error();
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @Operation(summary = "createMenu")
    public UniformResponse createMenu(@RequestBody @Validated MenuActionReqType menu){
        if(StrUtil.isEmpty(menu.name) || StrUtil.isEmpty(menu.title)
                || StrUtil.isEmpty(menu.path) || StrUtil.isEmpty(menu.component)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysMenuEntity duplicatedMenu = menuRepository.findByName(menu.name);
        if(duplicatedMenu!=null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        try {
            SysMenuEntity newMenu = new SysMenuEntity();
            //don't set ID for create
            newMenu.setPid(menu.pid);
            newMenu.setName(menu.name);
            newMenu.setTitle(menu.title);
            newMenu.setIcon(menu.icon);
            newMenu.setPath(menu.path);
            newMenu.setRedirect(menu.redirect);
            newMenu.setComponent(menu.component);
            newMenu.setSubReport(menu.subRpt);
            newMenu.setPos(menu.pos);
            newMenu.setActive(menu.active);
            newMenu.setDeleted(false);
            //created_by, create_time, updated_by and update_time are generated automatically by jpa (JpaAuditorAware)

            // save menu
            menuRepository.save(newMenu);

            // get this new menu and update path
            String menuPath = newMenu.getPath();
            if(menuPath.toLowerCase().contains(":id")){
                SysMenuEntity currentMenu = menuRepository.findByName(menu.name);
                // update menu path which include string ':id' to exact menu id
                menuPath = menuPath.replace(":id", currentMenu.getId().toString());
                currentMenu.setPath(menuPath);
                menuRepository.save(currentMenu);
            }

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
    @Operation(summary = "updateMenu")
    public UniformResponse updateMenu(@RequestBody @Validated MenuActionReqType menu){
        if(menu.id==0 || StrUtil.isEmpty(menu.name) || StrUtil.isEmpty(menu.title)
                || StrUtil.isEmpty(menu.path) || StrUtil.isEmpty(menu.component)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysMenuEntity targetEntity = menuRepository.findById(menu.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setPid(menu.pid);
            targetEntity.setName(menu.name);
            targetEntity.setTitle(menu.title);
            targetEntity.setIcon(menu.icon);
            targetEntity.setPath(menu.path);
            targetEntity.setComponent(menu.component);
            targetEntity.setRedirect(menu.redirect);
            targetEntity.setSubReport(menu.subRpt);
            targetEntity.setPos(menu.pos);
            targetEntity.setActive(menu.active);
            //targetEntity.setOrder(menu.order);
            //created_by, create_time, updated_by and update_time are generated automatically by jpa (JpaAuditorAware)

            // update menu
            menuRepository.save(targetEntity);
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
    @Operation(summary = "activeMenu")
    public UniformResponse activeMenu(@RequestBody @Validated ActiveReqType request){
        if(request.id==0){
            return UniformResponse.error(UniformResponseCode.API_EXCEPTION);
        }

        SysMenuEntity targetEntity = menuRepository.findById(request.id).get();
        if(targetEntity==null){
            //target role doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            // update active status
            targetEntity.setActive(request.active);
            menuRepository.save(targetEntity);
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
    @Operation(summary = "deleteMenu")
    public UniformResponse deleteMenu(@RequestParam @Validated Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysMenuEntity targetEntity = menuRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        }

        if(tokenIsAdmin) {
            if(targetEntity.getComponent().equals("/dashboard/index")){
                // admin can delete dashboard only
                List<Integer> orgList = reportRepository.findDistinctOrgByMenuId(targetEntity.getId());
                if (orgList.size() > 1 || !orgList.get(0).equals(tokenOrgId)) {
                    // admin can NOT delete a report which is not in his org
                    return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
                }
            } else {
                // no permit
                return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
            }
        }

        try {
            // set entity to obsolete
            targetEntity.setActive(false);
            targetEntity.setDeleted(true);
            menuRepository.save(targetEntity);
            //menuRepository.deleteById(id);

            return UniformResponse.ok();
        } catch (Exception e) {
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("dashboard")
    @PreAuthorize("hasAnyRole('Superuser', 'Administrator', 'Admin')")
    @Operation(summary = "getDashboard")
    public UniformResponse getDashboard(@RequestBody @Validated JSONObject request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        Integer id = Integer.parseInt(request.get("id").toString());

        SysMenuEntity queryEntities = menuRepository.findById(id).get();

        // build response
        List<DashboardListRspType> rspList = new ArrayList<DashboardListRspType>();
        for(VizReportEntity entity: queryEntities.getReports()){
            DashboardListRspType item = new DashboardListRspType();
            JSONArray pages = new JSONArray(entity.getPages());
            item.id = entity.getId();
            item.name = entity.getName();
            item.desc = entity.getDesc();
            item.type = entity.getType();
            item.pageCount = pages.size();
            item.updatedBy = entity.getUpdatedBy();
            item.updatedAt = entity.getUpdatedAt();
            rspList.add(item);
        }

        return UniformResponse.ok().data(rspList);
    }

    // set submenu to null in order to avoid endless loop
    private void SetMenuNull(List<SysMenuEntity> treeMenus){
        for(SysMenuEntity menu: treeMenus){
            if(menu.getChildren()!=null){
                SetMenuNull(menu.getChildren());
            }
            if(menu.getReports()!=null){
                for(VizReportEntity report: menu.getReports()){
                    report.setMenu(null);
                }
            }
        }
    }

}
