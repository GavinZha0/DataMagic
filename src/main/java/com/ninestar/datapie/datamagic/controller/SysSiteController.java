package com.ninestar.datapie.datamagic.controller;


import cn.hutool.core.util.StrUtil;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.ActiveReqType;
import com.ninestar.datapie.datamagic.bridge.DatasetActionReqType;
import com.ninestar.datapie.datamagic.bridge.MsgActionReqType;
import com.ninestar.datapie.datamagic.entity.SysMsgEntity;
import com.ninestar.datapie.datamagic.entity.SysSiteEntity;
import com.ninestar.datapie.datamagic.entity.SysUserEntity;
import com.ninestar.datapie.datamagic.entity.VizDatasetEntity;
import com.ninestar.datapie.datamagic.repository.SysMsgRepository;
import com.ninestar.datapie.datamagic.repository.SysSiteRepository;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
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
@Controller
@RequestMapping("/site")
public class SysSiteController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysSiteRepository siteRepository;

    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('Superuser')")
    @ApiOperation(value = "getSiteList", httpMethod = "POST")
    public UniformResponse getSiteList() {
        List<SysSiteEntity> targetEntities = siteRepository.findAll();
        return UniformResponse.ok().data(targetEntities);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @ApiOperation(value = "createSite", httpMethod = "POST")
    public UniformResponse createSite(@RequestBody @ApiParam(name = "request", value = "msg info") SysSiteEntity req){
        if(StrUtil.isEmpty(req.getName())){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        try {
            // save site
            siteRepository.save(req);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('Superuser')")
    @ApiOperation(value = "updateSite", httpMethod = "POST")
    public UniformResponse updateSite(@RequestBody @ApiParam(name = "req", value = "site info") SysSiteEntity req){
        if(req.id==0 || StrUtil.isEmpty(req.name)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        SysSiteEntity targetEntity = siteRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            targetEntity.setId(req.id);
            targetEntity.setName(req.name);
            targetEntity.setOwner(req.owner);
            targetEntity.setPartner(req.partner);
            targetEntity.setLogo(req.logo);
            targetEntity.setAbout(req.about);

            // update site
            siteRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('Superuser')")
    @ApiOperation(value = "deleteSite", httpMethod = "DELETE")
    public UniformResponse deleteSite(@RequestParam @ApiParam(name = "id", value = "site id") Integer id){
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }
        try {
            siteRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

}
