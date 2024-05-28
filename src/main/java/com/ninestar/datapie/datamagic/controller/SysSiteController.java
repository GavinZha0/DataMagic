package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.util.StrUtil;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.entity.SysSiteEntity;
import com.ninestar.datapie.datamagic.repository.SysSiteRepository;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import javax.annotation.Resource;
import java.util.List;

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
@Tag(name = "SysSite")
public class SysSiteController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysSiteRepository siteRepository;

    @PostMapping("/list")
    @PreAuthorize("hasAnyRole('Superuser')")
    @Operation(summary = "getSiteList")
    public UniformResponse getSiteList() {
        List<SysSiteEntity> targetEntities = siteRepository.findAll();
        return UniformResponse.ok().data(targetEntities);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @Operation(summary = "createSite")
    public UniformResponse createSite(@RequestBody @Validated SysSiteEntity req){
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
    @Operation(summary = "updateSite")
    public UniformResponse updateSite(@RequestBody @Validated SysSiteEntity req){
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
    @Operation(summary = "deleteSite")
    public UniformResponse deleteSite(@RequestParam @Validated Integer id){
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
