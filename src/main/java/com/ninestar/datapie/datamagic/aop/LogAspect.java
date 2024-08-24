package com.ninestar.datapie.datamagic.aop;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.entity.LogAccessEntity;
import com.ninestar.datapie.datamagic.entity.LogActionEntity;
import com.ninestar.datapie.datamagic.service.AsyncTaskService;
import com.ninestar.datapie.datamagic.utils.IpUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * 操作日志记录处理
 * 
 * @author ruoyi
 */
@Aspect
@Component
public class LogAspect
{
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /** 排除敏感属性字段 */
    public static final String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirm" };

    @Resource
    private AsyncTaskService asyncService;

    /**
     * 处理完请求后执行
     *
     * @param joinPoint 切点
     */
    @AfterReturning(pointcut = "@annotation(ctrlLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, LogAnn ctrlLog, Object jsonResult)
    {
        if(ctrlLog.logType()==LogType.ACCESS){
            handleAccLog(joinPoint, ctrlLog, null, jsonResult);
        }
        else if(ctrlLog.logType()==LogType.ACTION){
            handleActLog(joinPoint, ctrlLog, null, jsonResult);
        }
    }

    /**
     * 拦截异常操作
     * 
     * @param joinPoint 切点
     * @param e 异常
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, LogAnn controllerLog, Exception e)
    {
        handleAccLog(joinPoint, controllerLog, e, null);
    }

    protected void handleAccLog(final JoinPoint joinPoint, LogAnn ctrlLog, final Exception e, Object jsonResult)
    {
        Integer userId = null;
        String username = null;
        // create log
        LogAccessEntity accLog = new LogAccessEntity();

        try
        {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth!=null){
                // get user info from authentication normally
                Object userObj = auth.getPrincipal();
                //userId = Integer.parseInt(auth.getPrincipal().toString());
                userId = 2;
                username = auth.getCredentials().toString();
                accLog.setResult("ok");
            }
            else if(jsonResult!=null){
                // auth is null when first login
                // first login without SecurityContextHolder but with response
                String resp = JSONUtil.parseObj(jsonResult).get("principal").toString();
                userId = JSONUtil.parseObj(resp).getInt("id");
                username = JSONUtil.parseObj(resp).getStr("name");
                accLog.setResult("ok");
            }
            else{
                // failed to login
                JSONArray args = JSONUtil.parseArray(joinPoint.getArgs());
                for(Object arg: args) {
                    JSONObject argObj = JSONUtil.parseObj(arg);
                    username = argObj.getStr("principal");
                    userId = null;

                    if(StrUtil.isEmpty(e.getMessage())){
                        accLog.setResult("Failure");
                    }
                    else{
                        accLog.setResult(e.getMessage());
                    }
                }
            }

            // user info
            accLog.setUserId(userId);
            accLog.setUsername(username);

            /* use UTC time zone in everywhere
             * database format: timestamp
             * database: UTC (show variables like '%time_zone%' or select @@system_time_zone, @@time_zone, @@global.time_zone, @@session.time_zone)
             * JVM: UTC (TimeZone.setDefault(TimeZone.getTimeZone( "UTC"));)
             * Jackson: UTC (spring.jackson.time-zone=UTC)
             * OS: never mind
             * DB link of JDBC: UTC (serverTimezone=UTC)
             * JPA: set localtime via db interface (localtime equals UTC time because we set UTC to default system time zone)
             * client(js, browser) gets UTC time from DB then convert timestamp to datetime of user's time zone
             */


            // get time zone of this system (JVM, not OS)
            ZoneId zoneId = ZoneId.systemDefault();
            //zoneId = ZoneId.from(ZonedDateTime.now());

            // all available zones which can be used to show on GUI and allow user to select
            //Set<String> zoneIds = ZoneId.getAvailableZoneIds();
            //Set<String> filteredZones = zoneIds.parallelStream().filter( i -> !i.startsWith("UTC") && !i.startsWith("Etc") && !i.startsWith("GMT") && !i.startsWith("CET")).collect(Collectors.toSet());

            // suggest to use this method which is new after JDK8
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);
            // always indicate timezone (default is UTC for dataMagic)
            LocalDateTime localZonedNow = LocalDateTime.now(ZoneId.systemDefault());
            //LocalDateTime localZonedNow1 = LocalDateTime.now();
            //LocalDateTime utcZonedNow = LocalDateTime.now(Clock.systemUTC());
            // convert to timestamp for mysql
            Timestamp ts = Timestamp.valueOf(formatter.format(localZonedNow));
            //Timestamp timestamp = Timestamp.valueOf("2023-06-12 17:01:00");
            //Timestamp timestamp2 = new Timestamp(new Date().getTime());
            // UTC time
            accLog.setTsUtc(ts);

            /*
            LocalDateTime ldt = LocalDateTime.now();
            DateTimeFormatter  fmt1 = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.UK);
            System.out.println(fmt1.format(ldt));
            DateTimeFormatter  fmt2 = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(new Locale("en", "IN"));
            System.out.println(fmt2.format(ldt));
            DateTimeFormatter  fmt3 = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(new Locale("zh", "CN"));
            System.out.println(fmt3.format(ldt));
            */

            // get URL
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String uri = attributes.getRequest().getRequestURI();
            if(!StrUtil.isEmpty(uri) && uri.contains("logout")){
                accLog.setLogin(false);
            }
            else{
                // uri is null when first login
                accLog.setLogin(true);
            }

            // IP address
            String ip = IpUtils.getIpAddr(attributes.getRequest());
            accLog.setIp(ip);

            // get browser info
            UserAgent userAgent = UserAgentUtil.parse(attributes.getRequest().getHeader("User-Agent"));
            accLog.setBrowser(userAgent.getBrowser().getName());
            //accLog.setOs(userAgent.getOs().getName());
            accLog.setOs(userAgent.getPlatform().getName() + " " + userAgent.getOsVersion());

            // get language from header
            String lang = attributes.getRequest().getHeader("Lang-Id"); // user selected language of DataPie
            //accLog.setLang(attributes.getRequest().getLocale().getLanguage()); // accept language
            accLog.setLang(lang);

            // get zone Id from header
            String tz = attributes.getRequest().getHeader("Zone-Id");
            //ZoneOffset  zoneOffset = ZoneId.of(tz).getRules().getOffset(Instant.now());
            accLog.setTimeZone(tz);

            // 异步保存数据库
            asyncService.executeAccLog(accLog);
        }
        catch (Exception exp)
        {
            log.error("Exception:{}", exp.getMessage());
            exp.printStackTrace();
        }
    }


    protected void handleActLog(final JoinPoint joinPoint, LogAnn ctrlLog, final Exception e, Object jsonResult)
    {
        Integer userId = null;
        String username = null;
        LogActionEntity actLog = new LogActionEntity();

        try
        {
            // get user info
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if(auth!=null){
                // get user info from authentication normally
                userId = Integer.parseInt(auth.getPrincipal().toString());
                username = auth.getCredentials().toString();
            }
            else{
                // register
                JSONArray args = JSONUtil.parseArray(joinPoint.getArgs());
                for(Object arg: args) {
                    JSONObject argObj = JSONUtil.parseObj(arg);
                    username = argObj.getStr("username");
                    userId = null;

                    if(e!=null){
                        if(StrUtil.isEmpty(e.getMessage())){
                            actLog.setResult("Failure");
                        }
                        else{
                            actLog.setResult(e.getMessage());
                        }
                    }
                }
            }

            // user (no id for registering)
            actLog.setUserId(userId);
            actLog.setUsername(username);
            // UTC time
            actLog.setTsUtc(new Timestamp(System.currentTimeMillis()));
            actLog.setType(ctrlLog.actionType().toString());

            String className = joinPoint.getTarget().getClass().getName();
            String[] cls = className.split("[.]");
            if(cls.length>2){
                // reduce the length
                className = String.join(".", cls[cls.length-2], cls[cls.length-1]);
            }
            actLog.setModule(className);
            actLog.setMethod(joinPoint.getSignature().getName());

            // http request attributes
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            actLog.setUrl(attributes.getRequest().getRequestURI());

            if(ctrlLog.saveReq()){
                String requestMethod = attributes.getRequest().getMethod();
                if(requestMethod.equals("PUT") || requestMethod.equals("POST")){
                    String params = argsArrayToString(joinPoint.getArgs());
                    JSONObject jParams = new JSONObject(params);
                    for(Map.Entry<String, Object> entry: jParams.entrySet()){
                        if(Arrays.asList(EXCLUDE_PROPERTIES).contains(entry.getKey())){
                            entry.setValue(""); // set it to null like password
                        }
                    }
                    params = jParams.toString();
                    actLog.setParam(StrUtil.sub(params, 0, 1000));
                    if(joinPoint.getArgs().length>0){
                        Object[] args = joinPoint.getArgs();
                        for(Object item: args){
                            JSONObject jObj = new JSONObject(item);
                            actLog.setTid(jObj.getInt("id"));
                            break;
                        }
                    }
                }
                else if(requestMethod.equals("DELETE"))
                {
                    // delete have one parameter id
                    String id = attributes.getRequest().getParameter("id");
                    if(StrUtil.isNotEmpty(id)){
                        actLog.setParam("{'id':"+id+"}");
                        actLog.setTid(Integer.parseInt(id));
                    }
                }
            }

            actLog.setResult("ok");

            // save to DB
            asyncService.executeActLog(actLog);
        }
        catch (Exception exp)
        {
            log.error("Exception:{}", exp.getMessage());
            exp.printStackTrace();
        }
    }

    /**
     * 参数拼装
     */
    private String argsArrayToString(Object[] paramsArray)
    {
        String params = "";
        if (paramsArray != null && paramsArray.length > 0)
        {
            for (Object o : paramsArray)
            {
                if (o!=null && !isFilterObject(o))
                {
                    try
                    {
                        String jsonObj = JSONUtil.parseObj(o).toString();
                        params += jsonObj.toString() + " ";
                    }
                    catch (Exception e)
                    {
                    }
                }
            }
        }
        return params.trim();
    }

    /**
     * 判断是否需要过滤的对象。
     * 
     * @param o 对象信息。
     * @return 如果是需要过滤的对象，则返回true；否则返回false。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o)
    {
        Class<?> clazz = o.getClass();
        if (clazz.isArray())
        {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        }
        else if (Collection.class.isAssignableFrom(clazz))
        {
            Collection collection = (Collection) o;
            for (Object value : collection)
            {
                return value instanceof MultipartFile;
            }
        }
        else if (Map.class.isAssignableFrom(clazz))
        {
            Map map = (Map) o;
            for (Object value : map.entrySet())
            {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }
}
