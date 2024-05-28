package com.ninestar.datapie.datamagic.controller;


import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.bridge.DashboardListRspType;
import com.ninestar.datapie.datamagic.entity.SysMenuEntity;
import com.ninestar.datapie.datamagic.entity.VizReportEntity;
import com.ninestar.datapie.datamagic.repository.SysMenuRepository;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
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
 * @since 2021-11-13
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "SysDashboard")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SysDashboardController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysMenuRepository menuRepository;

    @PostMapping(value="{id}")
    @Operation(description = "getReports")
    public UniformResponse getReports(@PathVariable("id") Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        SysMenuEntity queryEntities = menuRepository.findById(id).get();

        // build response
        List<DashboardListRspType> rspList = new ArrayList<DashboardListRspType>();
        for(VizReportEntity entity: queryEntities.getReports()){
            if(tokenIsSuperuser || entity.getCreatedBy().equals(tokenUsername) || (entity.getOrg().getId().equals(tokenOrgId) && entity.getPublishPub())) {
                DashboardListRspType item = new DashboardListRspType();
                JSONArray pages = new JSONArray(entity.getPages());
                item.id = entity.getId();
                item.name = entity.getName();
                item.desc = entity.getDesc();
                item.type = entity.getType();
                item.pages = pages;
                item.pageCount = pages.size();
                item.updatedBy = entity.getUpdatedBy();
                item.updatedAt = entity.getUpdatedAt();
                rspList.add(item);
            }
        }

        return UniformResponse.ok().data(rspList);
    }
}
