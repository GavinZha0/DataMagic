package com.ninestar.datapie.datamagic.controller;

import cn.hutool.json.JSONObject;
import com.ninestar.datapie.datamagic.entity.SysRoleMenuPermitEntity;
import com.ninestar.datapie.datamagic.repository.SysRoleMenuPermitRepository;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.Set;

/**
 * <p>
 *  Controller
 * </p>
 *
 * @author Gavin.Zhao
 * @since 2021-11-22
 */
@RestController
@RequestMapping("/permit")
@Tag(name = "SysRolePermit")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class SysRoleMenuPermitController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private SysRoleMenuPermitRepository rolePermitRepository;


    @PostMapping("/list")
    @Operation(summary = "getPermits")
    public UniformResponse gePermits(@RequestBody @Parameter(description = "role id")JSONObject request) {
        //Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        //Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        Integer id = Integer.parseInt(request.get("id").toString());
        Set<SysRoleMenuPermitEntity> rolePermits = rolePermitRepository.findByRoleId(id);
        return UniformResponse.ok().data(rolePermits);
    }
}
