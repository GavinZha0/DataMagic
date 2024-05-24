package com.ninestar.datapie.datamagic.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.annotation.SingleReqParam;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.ImporterListRspType;
import com.ninestar.datapie.datamagic.bridge.ImporterUploadReqType;
import com.ninestar.datapie.datamagic.bridge.TableListReqType;
import com.ninestar.datapie.datamagic.bridge.TablePrepareReqType;
import com.ninestar.datapie.datamagic.entity.DataImportEntity;
import com.ninestar.datapie.datamagic.entity.DataSourceEntity;
import com.ninestar.datapie.datamagic.repository.DataImportRepository;
import com.ninestar.datapie.datamagic.repository.DataSourceRepository;
import com.ninestar.datapie.datamagic.repository.SysOrgRepository;
import com.ninestar.datapie.datamagic.service.AsyncTaskService;
import com.ninestar.datapie.datamagic.utils.DbUtils;
import com.ninestar.datapie.datamagic.utils.JpaSpecUtil;
import com.ninestar.datapie.datamagic.utils.MinioUtil;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  Controller
 * </p>
 *
 * @author Gavin.Zhao
 * @since 2022-02-16
 */
@RestController
@RequestMapping("/file")
@Api(tags = "File")
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class DataFileController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${web.upload-path}")
    private String uploadPath;

    @Resource
    public DataImportRepository importRepository;

    @Resource
    public MinioUtil minioUtil;

    @Resource
    public DataSourceRepository sourceRepository;

    @Resource
    private SysOrgRepository orgRepository;

    @Resource
    private AsyncTaskService asyncService;

    @Resource
    private DbUtils dbUtils;

    private static TablePrepareReqType tablePrepareInfo = new TablePrepareReqType();


    @PostMapping("/list")
    @ApiOperation(value = "getFileList", httpMethod = "POST", consumes = "application/json")
    //public UniformResponse getFileList(@SingleReqParam @ApiParam(name = "bucket", value = "bucket") String bucket) throws Exception {
    public UniformResponse getFileList() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        String bucket_prefix = "pie-org-";
        String bucket_name = "pie-org-" + String.valueOf(tokenOrgId);

        if(minioUtil.existsBucket(bucket_name)){
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.set("records", minioUtil.listFiles(bucket_name));
            return UniformResponse.ok().data(jsonResponse);
        } else {
            return UniformResponse.error(UniformResponseCode.TARGET_RESOURCE_NOT_EXIST);
        }
    }

    @PostMapping("/buckets")
    @ApiOperation(value = "getFileBuckets", httpMethod = "POST")
    public UniformResponse getFileBuckets() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("records", minioUtil.listBuckets());
        return UniformResponse.ok().data(jsonResponse);
    }

    @PostMapping("/upload")
    @ApiOperation(value = "fileUpload", httpMethod = "POST")
    public UniformResponse fileUpload(@RequestParam("config") String config, @RequestParam("files") MultipartFile[] files) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        JSONObject jsonObj = JSONUtil.parseObj(config);
        ImporterUploadReqType reqConfig = jsonObj.toBean(ImporterUploadReqType.class);

        if(reqConfig==null || reqConfig.source==null || files==null || files.length<=0){
            return UniformResponse.error();
        }

        // suggest to use this method which is new after JDK8
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.US);
        LocalDateTime utcNow = LocalDateTime.now(ZoneId.of("UTC"));
        String utcStr = formatter.format(utcNow);

        // insert a record
        DataImportEntity newEntity = new DataImportEntity();
        //don't set ID for creation
        newEntity.setFiles(JSONUtil.toJsonStr(reqConfig.fileNames));
        newEntity.setType(reqConfig.type);
        newEntity.setAttrs((new JSONObject(reqConfig.fileAttrs)).toString());
        newEntity.setFields(JSONUtil.toJsonStr(reqConfig.colFields));
        newEntity.setConfig((new JSONObject(reqConfig.dbConfig)).toString());
        newEntity.setOrg(orgRepository.findById(tokenOrgId).get());
        newEntity.setPubFlag(false);
        newEntity.setSourceId(reqConfig.source);
        newEntity.setTableName(reqConfig.table);
        newEntity.setOverwrite(reqConfig.overwrite);
        newEntity.setRows(reqConfig.rows);
        newEntity.setFtpPath(reqConfig.bucket);
        newEntity.setStatus("uploading");
        //create_time and update_time are generated automatically by jpa

        // save entity
        DataImportEntity savedEntity = importRepository.save(newEntity);

        Map<String, InputStream> fileMap  = new HashMap<>();
        for(MultipartFile partFile: files) {
            // get stream before it passed to async task
            // otherwise you will get "FileNotFoundException"
            fileMap.put(partFile.getOriginalFilename(), partFile.getInputStream());

        }

        // upload files to ftp and do ETL in async task
        asyncService.importFileTask(fileMap, "MINIO", reqConfig, savedEntity);
        return UniformResponse.ok();
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.DELETE)
    @DeleteMapping("/delete")
    @ApiOperation(value = "deleteImportRec", httpMethod = "DELETE")
    public UniformResponse deleteImportRec(@RequestParam @ApiParam(name = "id", value = "importer id") Integer id){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenIsSuperuser || tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        if(id==0){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        DataImportEntity targetEntity = importRepository.findById(id).get();
        if(targetEntity==null){
            //target entity doesn't exist
            return UniformResponse.ok();
        } else if(!tokenIsAdmin){
            return UniformResponse.error(UniformResponseCode.USER_NO_PERMIT);
        }

        try {
            // delete entity
            importRepository.deleteById(id);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            return UniformResponse.error(e.getMessage());
        }
    }

    @PostMapping("/progress")
    @ApiOperation(value = "getImportProgress", httpMethod = "POST")
    public UniformResponse getImportProgress() throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        if(tablePrepareInfo.fileInfo.size()==0){
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.set("progress", 100);
            return UniformResponse.ok().data(jsonResponse);
        };

        Integer total = 0;
        Integer fileCount = 0;
        for(int i=0; i<tablePrepareInfo.fileInfo.size(); i++){
            total += tablePrepareInfo.fileInfo.get(i).total;
            if(tablePrepareInfo.fileInfo.get(i).total>20){
                fileCount++;
            }
        }

        // 1 progress = 10000 records importing
        total = Math.round(total/10000);
        Integer imported = dbUtils.getProgress();
        Integer progress = 0;
        if(total>0){
            progress = Math.round(imported*100*fileCount/(total*tablePrepareInfo.fileInfo.size()));
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.set("progress", progress);
        return UniformResponse.ok().data(jsonResponse);
    }
}
