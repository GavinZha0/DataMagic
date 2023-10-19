package com.ninestar.datapie.datamagic.controller;


import cn.hutool.json.JSONArray;
import com.ninestar.datapie.datamagic.bridge.DashboardListRspType;
import com.ninestar.datapie.datamagic.entity.SysMenuEntity;
import com.ninestar.datapie.datamagic.entity.VizReportEntity;
import com.ninestar.datapie.datamagic.repository.SysMenuRepository;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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
@RequestMapping("/home")
@Api(tags = "Home")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SysHomeController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysMenuRepository menuRepository;

    @PostMapping(value="")
    @ApiOperation(value = "getHomePage", httpMethod = "POST")
    public UniformResponse getHomePage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        SysMenuEntity queryEntities = menuRepository.findByName("Home");
        if(queryEntities == null ){
            // home page is not set
            return UniformResponse.ok();
        }

        // build response
        DashboardListRspType homeReport = new DashboardListRspType();
        for(VizReportEntity entity: queryEntities.getReports()){
            if(entity.getOrg().getId().equals(tokenOrgId)) {
                JSONArray pages = new JSONArray(entity.getPages());
                homeReport.id = entity.getId();
                homeReport.name = entity.getName();
                homeReport.desc = entity.getDesc();
                homeReport.type = entity.getType();
                homeReport.pages = pages;
                homeReport.pageCount = pages.size();
                homeReport.updatedBy = entity.getUpdatedBy();
                homeReport.updatedAt = entity.getUpdatedAt();
                // only one home page
                break;
            }
        }
        return UniformResponse.ok().data(homeReport);
    }
}
