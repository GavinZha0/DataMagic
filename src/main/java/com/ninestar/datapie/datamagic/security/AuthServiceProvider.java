package com.ninestar.datapie.datamagic.security;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.ninestar.datapie.datamagic.aop.LogAnn;
import com.ninestar.datapie.datamagic.aop.LogType;
import com.ninestar.datapie.datamagic.bridge.AuthLoginRspType;
import com.ninestar.datapie.datamagic.entity.SysRoleEntity;
import com.ninestar.datapie.datamagic.entity.SysUserEntity;
import com.ninestar.datapie.datamagic.repository.SysUserRepository;
import com.ninestar.datapie.framework.consts.UniformResponseCode;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

/**
 * Logon authentication
 * @author Gavin.Zhao
 * real authentication process here
**/
@Component
public class AuthServiceProvider implements AuthenticationProvider {
    @Resource
    public SysUserRepository userRep;

    @Resource
    private PasswordEncoder pswEncoder;

    @Resource
    private Cache<String, Object> localCache;

    // LogAnn doesn't work when first login because authenticate is built after this
    // Gavin ???
    @LogAnn(logType = LogType.ACCESS)
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // Get identity and passcode
        String identity = (String) authentication.getPrincipal();
        String passcode = (String) authentication.getCredentials();
        SysUserEntity loginUser = null;

        // Doesn't get identity/passcode from client (http request)
        if(StrUtil.isEmpty(identity) || StrUtil.isEmpty(passcode)){
            throw new BadCredentialsException(UniformResponseCode.USER_AUTH_FAILURE.getMsg());
        }

        // verify captcha with cached code. Gavin???

        // detect identity
        if(Validator.isMobile(identity)){
            loginUser = userRep.findByPhone(identity);
        } else if(Validator.isEmail(identity)){
            loginUser = userRep.findByEmail(identity);
        } else {
            loginUser = userRep.findByName(identity);
        }

        // User doesn't exist
        if (loginUser == null) {
            throw new UsernameNotFoundException(UniformResponseCode.USER_NOT_EXIST.getMsg());
        }

        // find auth code from local cache
        Object objCode = localCache.getIfPresent(loginUser.getName());
        if(objCode!=null){
            String authCode = objCode.toString();
            // Check auth code
            if(!authCode.equals(passcode)){
                throw new BadCredentialsException(UniformResponseCode.USER_AUTH_FAILURE.getMsg());
            } else {
                // delete auth code after it is used
                localCache.invalidate(loginUser.getName());
            }
        } else {
            // Check password
            if (!pswEncoder.matches(passcode, loginUser.getPassword())) {
                throw new BadCredentialsException(UniformResponseCode.USER_AUTH_FAILURE.getMsg());
            }
        }

        // user is not active
        if (!loginUser.getActive()){
            throw new LockedException(UniformResponseCode.USER_IS_FROZEN.getMsg());
        }

        // no role(permit)
        if(loginUser.getRoles()==null || loginUser.getRoles().size()==0){
            throw new DisabledException(UniformResponseCode.USER_NO_PERMIT.getMsg());
        }

        AuthLoginRspType userInfo = new AuthLoginRspType();
        BeanUtil.copyProperties(loginUser, userInfo);
        if(StrUtil.isEmpty(userInfo.avatar) && StrUtil.isNotEmpty(loginUser.getEmail())){
            // get avatar from gravatar based on email address
            String emailHash = DigestUtils.md5Hex(loginUser.getEmail().toLowerCase().trim());
            userInfo.avatar = "http://www.gravatar.com/avatar/" + emailHash;
        }
        userInfo.orgId = loginUser.getOrg().getId();
        userInfo.orgName = loginUser.getOrg().getName();

        int i = 0;
        Set<GrantedAuthority> authorities = new HashSet<>();
        for(SysRoleEntity role: loginUser.getRoles()){
            userInfo.roleId.add(role.getId());
            userInfo.roleName.add(role.getName());
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            i++;
        }

        // Authentication method is form with username/password, so build UsernamePasswordAuthToken
        // This auth token has all user info including id, name, role and org.
        // This will be passed to successfulAuthentication
        UsernamePasswordAuthenticationToken userPwdAuthToken = new UsernamePasswordAuthenticationToken(userInfo, passcode, authorities);
        return userPwdAuthToken;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // current auth method is form with username/password
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
