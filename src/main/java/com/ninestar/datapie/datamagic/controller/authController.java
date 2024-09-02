package com.ninestar.datapie.datamagic.controller;


import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.ninestar.datapie.datamagic.aop.ActionType;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.AuthLoginRspType;
import com.ninestar.datapie.datamagic.bridge.AuthRegisterReqType;
import com.ninestar.datapie.datamagic.entity.SysMenuEntity;
import com.ninestar.datapie.datamagic.entity.SysRoleEntity;
import com.ninestar.datapie.datamagic.entity.SysUserEntity;
import com.ninestar.datapie.datamagic.entity.VizReportEntity;
import com.ninestar.datapie.datamagic.repository.*;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import com.ninestar.datapie.framework.utils.TreeUtils;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;


/**
 * <p>
 *  Auth Controller
 * </p>
 *
 * @author Gavin.Zhao
 * @since 2021-08-19
 */

// @RestController = @Controller + @ResponseBody
@RestController // return json or xml data (@Controller is used to return html or jsp)
@RequestMapping("/auth")
@Tag(name = "SysAuth") // Swagger 3
@CrossOrigin(originPatterns = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT})
public class authController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(100,50,4,20);
    private final CircleCaptcha circleCaptcha = CaptchaUtil.createCircleCaptcha(100, 50, 4, 20);
    private final ShearCaptcha shearCaptcha = CaptchaUtil.createShearCaptcha(100, 50, 4, 5);

    @Resource
    private SysUserRepository userRepository;

    @Resource
    private SysOrgRepository orgRepository;

    @Resource
    private SysRoleRepository roleRepository;

    @Resource
    private SysMenuRepository menuRepository;

    @Resource
    private Cache<String, Object> localCache;

    @Resource
    private SysRoleMenuPermitRepository rolePermitRepository;

    @Resource
    private PasswordEncoder pswEncoder;


    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/login")
    @Operation(summary = "login")
    public UniformResponse login(@RequestParam("username") String username, @RequestParam("password") String password) {
        // This is not reachable
        // This api is just for Knife4j to get token for interface test
        // use your username and password to LOGIN and get TOKEN from response
        // then put TOKEN into DocumentHelper/GlobalParams.
        // Authorization = 'Bearer ' + TOKEN
        return UniformResponse.ok();
    }

    @PostMapping("/permit")
    @Operation(summary = "getUserPermit")
    public UniformResponse getUserPermit() throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUsername = auth.getCredentials().toString();
        List<String> tokenRoles = auth.getAuthorities().stream().map(role->role.getAuthority()).collect(Collectors.toList());
        Boolean tokenIsSuperuser = tokenRoles.contains("ROLE_Superuser");
        Boolean tokenIsAdmin = tokenRoles.contains("ROLE_Administrator") || tokenRoles.contains("ROLE_Admin");

        List<SysMenuEntity> activeMenus = menuRepository.findByOrgIdActiveAndDeleted(tokenOrgId, true, false);
        List<SysMenuEntity> treeMenus = TreeUtils.buildTree(activeMenus, "id", "pid", "children");
        List<Map> menuPermit = rolePermitRepository.findPermitByUserId(tokenUserId);
        filterPermitMenus(treeMenus, menuPermit);




        // custom query of Mybatis
        // query result can be easily mapped to custom class like AuthPermitRspType
        // this is easier than Jpa
        //List<AuthPermitRspType> permitMenus = permitMapper.getPermitsByUserId(tokenUserId);

        //build tree list
        //List<AuthPermitRspType> permitTreeMenus = TreeUtils.buildTree(permitMenus, "id", "pid", "children");

        // custom native query(sql) of data JPA
        // result can be mapped to Object array, then you need to put them into custom class manually
        // or you have to define a VO class and map query result to it
        // or you have to use HQL to query and map result to custom class like Mybatis does
        //List<Object[]> userPermit = rolePermitRepository.findPermitByUser(tokenUserId);

        return UniformResponse.ok().data(treeMenus);
    }

    private void filterPermitMenus(List<SysMenuEntity> menus, List<Map> permits) {
        for(int i=menus.size()-1; i>=0; i--){
            SysMenuEntity menu = menus.get(i);
            Boolean isFound = false;
            String pmt = "";
            for(Map permit: permits){
                if(Long.parseLong(permit.get("id").toString())==menu.getId()){
                    pmt = permit.get("permit").toString();
                    menu.setPermit(Byte.valueOf(pmt)); // Gavin ---???
                    isFound = true;
                    break;
                }
            }
            if(isFound){
                SetSubMenuPermit(menus, pmt);
            }
            else{
                if(menu.getChildren()==null){
                    menus.remove(i);
                }
                else{
                    filterPermitMenus(menu.getChildren(), permits);
                }
            }

        }
    }

    // set submenu to null in order to avoid endless loop
    private void SetSubMenuPermit(List<SysMenuEntity> treeMenus, String permit){
        for(SysMenuEntity menu: treeMenus){
            menu.setPermit(Byte.valueOf(permit)); // Gavin --???
            if(menu.getChildren()!=null){
                SetSubMenuPermit(menu.getChildren(), permit);
            }
            if(menu.getReports()!=null){
                for(VizReportEntity report: menu.getReports()){
                    // avoid endless loop
                    report.setMenu(null);
                }
            }
        }
    }

    @PostMapping("/info")
    @Operation(summary = "getUserInfo")
    public UniformResponse getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        //Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        SysUserEntity loginUser = userRepository.findById(tokenUserId).get();

        if(loginUser==null){
            return UniformResponse.error(UniformResponseCode.USER_NOT_EXIST);
        }
        else if(!loginUser.getActive()){
            return UniformResponse.error(UniformResponseCode.USER_IS_FROZEN);
        }

        AuthLoginRspType userInfo = new AuthLoginRspType();
        BeanUtil.copyProperties(loginUser, userInfo);
        userInfo.orgId = loginUser.getOrg().getId();
        userInfo.orgName = loginUser.getOrg().getName();

        for(SysRoleEntity role: loginUser.getRoles()){
            userInfo.roleId.add(role.getId());
            userInfo.roleName.add(role.getName());
        }

        return UniformResponse.ok().data(userInfo);
    }

    @GetMapping("/captcha")
    @Operation(summary = "getCaptcha")
    public void getCaptcha(HttpServletResponse response) throws IOException {
        // set response header
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setContentType("image/png");
        response.setDateHeader("Expires", 0);
        response.setCharacterEncoding("UTF-8");

        // get output stream
        OutputStream stream = response.getOutputStream();

        // generate captcha and put into stream
        Integer captchaType = (int) (Math.random() * 3);
        switch (captchaType) {
            case 0:
                shearCaptcha.createCode();
                shearCaptcha.write(stream);
                break;
            case 1:
                circleCaptcha.createCode();
                circleCaptcha.write(stream);
                break;
            default:
                lineCaptcha.createCode();
                lineCaptcha.write(stream);
                break;
        }

        // save code into cache
        String code = circleCaptcha.getCode();

        // close stream
        stream.flush();
        stream.close();
    }

    @PostMapping("/code")
    @Operation(summary = "getAuthCode")
    public UniformResponse getAuthCode(@RequestBody @Validated JSONObject identity) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // username, phone or email
        String id = identity.get("id").toString();
        SysUserEntity loginUser = null;

        if(Validator.isMobile(id)){
            loginUser = userRepository.findByPhone(id);
        } else if(Validator.isEmail(id)){
            loginUser = userRepository.findByEmail(id);
        } else {
            loginUser = userRepository.findByName(id);
        }

        if(loginUser==null){
            return UniformResponse.error(UniformResponseCode.USER_NOT_EXIST);
        }
        else if(!loginUser.getActive()){
            return UniformResponse.error(UniformResponseCode.USER_IS_FROZEN);
        }

        // generate auth code (6 digits)
        Integer authCode = (int)((Math.random()*9+1)*100000);
        authCode = 952768; // fixed auth code for debug only. Gavin !!!

        if(loginUser.getSmsCode()){
            if(StrUtil.isNotEmpty(loginUser.getPhone())){
                // trigger sms system to send auth code
            }
        } else {
            if(StrUtil.isNotEmpty(loginUser.getEmail())){
                // trigger email system to send auth code
            }
        }
        localCache.put(loginUser.getName(), authCode);
        return UniformResponse.ok();
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.ADD)
    @PostMapping("/register")
    @Operation(summary = "register")
    public UniformResponse register(@RequestBody @Validated AuthRegisterReqType user){
        //Hibernate: insert into sys_user (active, avatar, create_time, created_by, deleted, department, email, name, realname, org_id, password, phone, update_time, updated_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        //Hibernate: insert into sys_user_role (user_id, role_id) values (?, ?)

        if(StrUtil.isEmpty(user.username) || StrUtil.isEmpty(user.password) || StrUtil.isEmpty(user.contact)){
            return UniformResponse.error(UniformResponseCode.REQUEST_INCOMPLETE);
        }

        if(!user.password.equals(user.confirm)){
            return UniformResponse.error(UniformResponseCode.PASSWORD_UNMATCHED);
        }

        SysUserEntity duplicatedUser = userRepository.findByName(user.username);
        if(duplicatedUser!=null){
            return UniformResponse.error(UniformResponseCode.USER_HAS_EXISTED);
        }

        try {
            SysUserEntity newUser = new SysUserEntity();
            Random r = new Random();

            //don't set ID for creating
            newUser.setName(user.username);
            newUser.setPassword(pswEncoder.encode(user.password));
            newUser.setRealname(user.username); // default realname
            if(Validator.isEmail(user.contact)){
                newUser.setEmail(user.contact);
            }
            else {
                newUser.setPhone(user.contact);
            }
            newUser.setActive(true); // active by default
            newUser.setDeleted(false);
            newUser.setCreatedBy(user.username); // register by himself
            newUser.setCreatedAt(new Timestamp(System.currentTimeMillis()));

            // get and fill with default org
            newUser.setOrg(orgRepository.findById(1).get());

            // get and fill with default roles
            newUser.getRoles().add(roleRepository.findById(1).get());

            // save user and roles
            userRepository.save(newUser);
            return UniformResponse.ok();
        }
        catch (Exception e){
            logger.error(e.toString());
            e.printStackTrace();
            return UniformResponse.error(e.getMessage());
        }
    }

    @LogAnn(logType = LogType.ACTION, actionType = ActionType.UPDATE)
    @PostMapping("/language")
    @Operation(summary = "switchLanguage")
    public UniformResponse switchLanguage(@RequestBody @Validated JSONObject request){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        String tokenUser = auth.getCredentials().toString();
        Boolean tokenSuperuser = auth.getAuthorities().contains("ROLE_superuser");
        Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        // we can switch language here
        // but we don't have to because this has been done by LocaleChangeInterceptor
        // we don't want to add 'lang' to all url of http request so we defined this specific api to switch language
        //LocaleContextHolder.setLocale(StringUtils.parseLocale(lang));

        return UniformResponse.ok();
    }

    @LogAnn(logType = LogType.ACCESS)
    @PostMapping("/logout")
    @Operation(summary = "logout")
    public UniformResponse logout(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer tokenUserId = Integer.parseInt(auth.getPrincipal().toString());
        //String tokenUser = auth.getCredentials().toString();
        //Integer tokenOrgId = Integer.parseInt(auth.getDetails().toString());

        return UniformResponse.ok();
    }
}
