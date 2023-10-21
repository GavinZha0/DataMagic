package com.ninestar.datapie.datamagic.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.*;
import com.ninestar.datapie.datamagic.entity.*;
import com.ninestar.datapie.datamagic.repository.*;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.datamagic.utils.SqlUtils;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.model.ColumnField;
import com.ninestar.datapie.framework.model.TreeSelect;
import com.ninestar.datapie.datamagic.utils.DbUtils;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 *  Controller
 * </p>
 *
 * @author Gavin.Zhao
 * @since 2021-10-30
 */
@RestController
@RequestMapping("/datareport")
@Api(tags = "Datareport")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class VizReportController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    public VizDatareportRepository reportRepository;

    @Resource
    public VizDataviewRepository dataviewRepository;

    @Resource
    public VizDatasetRepository datasetRepository;

    @Resource
    public SysMenuRepository menuRepository;

    @Resource
    private SysOrgRepository orgRepository;

    @Resource
    private DbUtils dbUtils;

    @PostMapping("/list")
    @ApiOperation(value = "getReportList", httpMethod = "POST")
    public UniformResponse getReportList(@RequestBody @ApiParam(name = "req", value = "request") TableListReqType req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        Long totalRecords = 0L;
        Page<VizReportEntity> pageEntities = null;
        List<VizReportEntity> queryEntities = null;

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
        Specification<VizReportEntity> specification = JpaSpecUtil.build(tokenOrgId,tokenIsSuperuser, tokenUsername, req.filter, req.search);

        // query data from database
        if(pageable!=null){
            pageEntities = reportRepository.findAll(specification, pageable);
            queryEntities = pageEntities.getContent();
            totalRecords = pageEntities.getTotalElements();
        }
        else{
            queryEntities = reportRepository.findAll(specification);
            totalRecords = (long) queryEntities.size();
        }

        // build response
        List<DatareportListRspType> rspList = new ArrayList<DatareportListRspType>();
        for(VizReportEntity entity: queryEntities){
            if(tokenIsSuperuser || entity.getCreatedBy().equals(tokenUsername) || entity.getPubFlag()) {
                DatareportListRspType item = new DatareportListRspType();
                BeanUtil.copyProperties(entity, item, new String[]{"page", "menu"});
                item.id = entity.getId();
                item.pubFlag = entity.getPubFlag();
                item.publishPub = entity.getPublishPub();
                item.viewIds = JSONUtil.parseArray(entity.getViewIds()).toList(Integer.class);
                item.pages = new JSONArray(entity.getPages()); // String to Json Array
                item.pageCount = item.pages.size();
                if (entity.getMenu() != null) {
                    item.menuId = entity.getMenu().getId();
                    item.menuName = entity.getMenu().getName();
                    item.menuTitle = entity.getMenu().getTitle();
                }
                rspList.add(item);
            }
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", rspList);
        jsonResponse.set("total", totalRecords);
        jsonResponse.set("current", req.page.current);

        return UniformResponse.ok().data(jsonResponse);
    }


    @PostMapping("/tree")
    @ApiOperation(value = "getReportTree", httpMethod = "POST")
    public UniformResponse getReportTree(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        // jpa page is starting with 0
        List<VizReportEntity> reportEntities = reportRepository.findAll();

        // convert list to tree by category
        Map<String, List<VizReportEntity>> reportMap = reportEntities.stream().collect(Collectors.groupingBy(t -> t.getGroup()));
        List<TreeSelect> treeReports = new ArrayList<>();
        Integer i = 1000;
        for(String group : reportMap.keySet()){
            TreeSelect treeGroup = new TreeSelect(i, "group", group, group, false, false);
            for(VizReportEntity report: reportMap.get(group)){
                TreeSelect treeNode = new TreeSelect(report.getId(), "report", report.getName(), report.getName(), true, false);
                JSONArray pages = new JSONArray(report.getPages());
                for(Object page: pages){
                    JSONObject jsonObj = JSONUtil.parseObj(page);
                    ReportPageType pg = jsonObj.toBean(ReportPageType.class);
                    TreeSelect treeLeaf = new TreeSelect(report.getId()*100+pg.id, "page", pg.name, pg.name, true, true);
                    treeNode.getChildren().add(treeLeaf);
                }
                treeGroup.getChildren().add(treeNode);
            }
            treeReports.add(treeGroup);
            i+=100;
        }

        return UniformResponse.ok().data(treeReports);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/create")
    @ApiOperation(value = "createDatareport", httpMethod = "POST")
    public UniformResponse createDatareport(@RequestBody @ApiParam(name = "req", value = "report info") DatareportActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");

        if(StrUtil.isEmpty(req.name) || req.pages.size()==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        List<VizReportEntity> duplicatedEntities = reportRepository.findByNameAndGroup(req.name, req.group);
        if(duplicatedEntities!=null && duplicatedEntities.size()>0){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_EXIST);
        }

        try {
            VizReportEntity newEntity = new VizReportEntity();
            //don't set ID for create
            newEntity.setName(req.name);
            newEntity.setDesc(req.desc);
            newEntity.setGroup(req.group);
            newEntity.setType(req.type);
            newEntity.setPages(req.pages.toString());
            newEntity.setViewIds(req.viewIds.toString());
            newEntity.setPubFlag(false);// default value
            newEntity.setPublishPub(false);// default value
            newEntity.setOrg(orgRepository.findById(tokenOrgId).get());
            //create_time and update_time are generated automatically by jpa

            // save source
            reportRepository.save(newEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/update")
    @ApiOperation(value = "updateDatareport", httpMethod = "POST")
    public UniformResponse updateDatareport(@RequestBody @ApiParam(name = "req", value = "Report info") DatareportActionReqType req){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(req.id==0 || StrUtil.isEmpty(req.name) || req.pages==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizReportEntity targetEntity = reportRepository.findById(req.id).get();
        if(targetEntity==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        } else if(!targetEntity.getCreatedBy().equals(tokenUsername)){
            // only author can update it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            targetEntity.setId(req.id);
            targetEntity.setName(req.name);
            targetEntity.setDesc(req.desc);
            targetEntity.setGroup(req.group);
            targetEntity.setType(req.type);
            targetEntity.setPages(req.pages.toString());
            targetEntity.setPubFlag(req.pubFlag);
            targetEntity.setViewIds(req.viewIds.toString());
            //create_time and update_time are generated automatically by jpa

            // update report
            reportRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.CLONE)
    @PostMapping("/clone")
    @ApiOperation(value = "cloneDatareport", httpMethod = "POST")
    public UniformResponse cloneDatareport(@RequestBody @ApiParam(name = "param", value = "report id") JSONObject request){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(request.get("id").toString());
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizReportEntity targetEntity = reportRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        String copyName = targetEntity.getName();
        List<VizReportEntity> targetCopies = reportRepository.findByNameContainingOrderByIdDesc(copyName+"(");
        if(targetCopies.size()>0){
            String tmp = targetCopies.get(0).getName();
            tmp = tmp.substring(tmp.indexOf("(")+1, tmp.indexOf(")"));
            Pattern pattern = Pattern.compile("[0-9]*");
            if(pattern.matcher(tmp).matches())
            {
                Integer idx = Integer.parseInt(tmp)+1;
                copyName += "(" + idx.toString() + ")";
            }
            else{
                copyName += "(1)";
            }
        }
        else{
            copyName += "(1)";
        }

        try {
            // update public status
            targetEntity.setId(0);
            targetEntity.setName(copyName);
            targetEntity.setMenu(null); // remove published attr from cloned entity
            targetEntity.setPublishPub(true);//default value
            reportRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/cancel")
    @ApiOperation(value = "cancelPublish", httpMethod = "POST")
    public UniformResponse cancelPublish(@RequestBody @ApiParam(name = "param", value = "report id") JSONObject request){
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        Integer id = Integer.parseInt(request.get("id").toString());
        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizReportEntity targetEntity = reportRepository.findById(id).get();
        if(targetEntity==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        try {
            // update public status
            targetEntity.setMenu(null);
            targetEntity.setPublishPub(true);//default value
            reportRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/execute")
    @ApiOperation(value = "execute", httpMethod = "POST")
    public UniformResponse execute(@RequestBody @ApiParam(name = "request", value = "Report Id and page Id") ReportExeReqType request) throws Exception {
        String loginUser = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
        String orgId = SecurityContextHolder.getContext().getAuthentication().getDetails().toString();

        if(request.reportId==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizReportEntity reportEntity = reportRepository.findById(request.reportId).get();
        if(reportEntity==null || reportEntity.getPages()==null){
            //target doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        DatareportPageRspType response = new DatareportPageRspType();
        // get pages
        JSONArray reportPages = new JSONArray(reportEntity.getPages());
        // find request page
        for(Object item: reportPages){
            JSONObject jsonPage = JSONUtil.parseObj(item);
            // covert JSONObject to Bean
            DatareportPageRspType page = jsonPage.toBean(DatareportPageRspType.class);
            if(page.id==request.pageId){
                // get the page
                response = page;
                response.filter = new JSONArray(jsonPage.get("filter"));
                response.grids = new JSONArray(jsonPage.get("grid"));
                break;
            }
        }

        if(response.grids==null){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        for(Object item: response.grids){
            JSONObject jsonGrid = JSONUtil.parseObj(item);
            // convert grid to bean
            DatareportPageRspType.DatareportGridType grid = jsonGrid.toBean(DatareportPageRspType.DatareportGridType.class);
            if(grid.type.equalsIgnoreCase("VIEW")){
                VizViewEntity viewEntity = dataviewRepository.findById(grid.id).get();
                if(viewEntity!=null){
                    // execute view and get response
                    //DatasetResultType viewData = VizViewController.execute(viewEntity.getId());
                    //response.views.add(viewData);

                    DatareportPageRspType.DatareportViewType pageView = new DatareportPageRspType.DatareportViewType();
                    pageView.id = viewEntity.getId();
                    pageView.type = viewEntity.getType();
                    pageView.libName = viewEntity.getLibName();
                    pageView.libCfg = new JSONObject(viewEntity.getLibCfg());
                    VizDatasetEntity datasetEntity = viewEntity.getDataset();
                    DataSourceEntity sourceEntity = datasetEntity.getDatasource();

                    String selectSqlQuery = datasetEntity.getQuery();
                    // translate variable of sql
                    selectSqlQuery = SqlUtils.sqlTranslate(selectSqlQuery, new JSONArray(datasetEntity.getVariable()));
                    if(StrUtil.count(selectSqlQuery, "@")>0 || StrUtil.count(selectSqlQuery, "$")>0){
                        // not all variables are mapped to exact value
                        return UniformResponse.error("Unknown variables in dataset query!");
                    }

                    // verify selection sql
                    UniformResponseCode validSql = SqlUtils.sqlValidate(selectSqlQuery, sourceEntity.getType());
                    if(validSql!=UniformResponseCode.SUCCESS){
                        return UniformResponse.error(validSql);
                    }

                    if(!dbUtils.isSourceExist(sourceEntity.getId())){
                        String pwd = new String(Base64.decode(sourceEntity.getPassword()));
                        dbUtils.add(sourceEntity.getId(), sourceEntity.getName(), sourceEntity.getType(), sourceEntity.getUrl(), sourceEntity.getParams(), sourceEntity.getUsername(), pwd);
                    }









                    List<ColumnField> cols = new ArrayList<>();
                    List<Object[]> result = new ArrayList<>();
                    try {
                        dbUtils.execute(sourceEntity.getId(), selectSqlQuery, cols, result);
                        pageView.recordField = cols;
                        pageView.records = result;
                        response.views.add(pageView);
                    } catch (Exception e) {
                        throw new Exception(e.getMessage());
                    }
                }
            }
        }

        return UniformResponse.ok().data(response);
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.SHARE)
    @PostMapping("/public")
    @ApiOperation(value = "publicReport", httpMethod = "POST")
    public UniformResponse publicReport(@RequestBody @ApiParam(name = "request", value = "Report id and pub flag") PublicReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(request.id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizReportEntity targetEntity = reportRepository.findById(request.id).get();
        if(targetEntity==null){
            //target user doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        } else if(!targetEntity.getCreatedBy().equals(tokenUsername) && !tokenIsAdmin){
            // only author can do it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // update public status
            targetEntity.setPubFlag(request.pub);
            reportRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.GRANT)
    @PostMapping("/publish")
    @ApiOperation(value = "publishReport", httpMethod = "POST")
    public UniformResponse publishReport(@RequestBody @ApiParam(name = "id", value = "Report id") PublishReqType request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(request.id==null){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        VizReportEntity targetEntity = reportRepository.findById(request.id).get();
        if(targetEntity==null){
            //target report doesn't exist
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }


        if(!tokenIsAdmin && !targetEntity.getCreatedBy().equalsIgnoreCase(tokenUsername)){
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        if(request.menuId==null){
            if(targetEntity.getMenu()==null){
                // no change
                return UniformResponse.ok();
            }
            else{
                // remove report from menu
                targetEntity.setMenu(null);
                targetEntity.setPublishPub(false);//default
            }
        }
        else {
            if(targetEntity.getMenu()!=null && targetEntity.getMenu().getId() == request.menuId && targetEntity.getPublishPub() == request.publishPub){
                // no change
                return UniformResponse.ok();
            }
            else{
                // publish report to another menu
                SysMenuEntity menuEntity = menuRepository.findById(request.menuId).get();
                if(menuEntity==null){
                    return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
                } else if(menuEntity.getName().equalsIgnoreCase("home")){
                    Set<VizReportEntity> prevHomeReport = menuEntity.getReports();
                    for(VizReportEntity report: prevHomeReport){
                        if(report.getOrg().getId().equals(tokenOrgId)){
                            // remove old home page of your org
                            report.setMenu(null);
                            reportRepository.save(report);
                        }
                    }

                    // replace home report
                    targetEntity.setMenu(menuEntity);
                    targetEntity.setPublishPub(true);
                } else {
                    // bind report to specific menu
                    targetEntity.setMenu(menuEntity);
                    targetEntity.setPublishPub(request.publishPub);
                }
            }
        }

        try {
            reportRepository.save(targetEntity);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @ApiOperation(value = "deleteReport", httpMethod = "DELETE")
    public UniformResponse deleteReport(@RequestParam @ApiParam(name = "id", value = "Report id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(id==0){
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }

        VizReportEntity targetEntity = reportRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        } else if(!tokenIsAdmin && !targetEntity.getCreatedBy().equals(tokenUsername)){
            // only author can do it
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // delete entity
            reportRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/groups")
    @ApiOperation(value = "getGroups", httpMethod = "POST")
    public UniformResponse getGroups() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        List<Object> distinctGroups = reportRepository.findDistinctGroup();

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", distinctGroups);
        return UniformResponse.ok().data(jsonResponse);
    }
}
