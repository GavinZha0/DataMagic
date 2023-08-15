package com.ninestar.datapie.datamagic.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.bridge.AuthLoginReqType;
import com.ninestar.datapie.datamagic.bridge.AuthLoginRspType;
import com.ninestar.datapie.datamagic.config.JwtConfig;
import com.ninestar.datapie.datamagic.utils.JwtTokenUtil;
import com.ninestar.datapie.framework.utils.UniformResponse;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;

import static com.ninestar.datapie.framework.consts.UniformResponseCode.*;

/**
 * Logon authentication
 * @author Gavin.Zhao
 * This is used to get username/password from http when url is login form via post
 * The default filter get username/password from request body(url)
 * It doesn't work for me because we put username/password into request payload(json)
 * So I have to override this filter to extract username/password by myself
 *
 **/
public class UsernamePasswordAuthFilter extends UsernamePasswordAuthenticationFilter {
    // custom auth method
    private AuthenticationManager authenticationManager;
    public UsernamePasswordAuthFilter(AuthenticationManager authenticationManager){
        this.authenticationManager = authenticationManager;

        //filter path and method for login
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/auth/login", "POST"));
    }

    /**
     * Get username/password from Http request payload and verify
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        AuthLoginReqType loginUser = new AuthLoginReqType();

        //get username/password from request body(url)
        // but the identity and passcode are not in body
        // the default UsernamePara is 'username' and PasswordPara is 'password'
        // they can be set to different value by setUsernameParameter() and setPasswordParameter()
        loginUser.identity = request.getParameter(this.getUsernameParameter());
        loginUser.passcode = request.getParameter(this.getPasswordParameter());

        if(StrUtil.isEmpty(loginUser.identity) && StrUtil.isEmpty(loginUser.passcode)) {
            // Read stream json data from request
            try {
                // username and password are in stream
                // identity may be username, phone number or email address
                // passcode may be password or dynamic auth code
                // so the name identity and passcode are being used here.
                loginUser = JSONUtil.parseObj(request.getInputStream()).toBean(AuthLoginReqType.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // auth token will be passed to auth provider to verify
        // this auth token include username and password only
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUser.identity, loginUser.passcode));
    }

    /**
     * Success to authenticate
     * Generate jwt token and send to client via http header
     */
    // login successfully
    // login log can be added here but annotation LogAnn doesn't work here
    // Gavin ???
    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication auth) throws IOException {

        // Generate jwt token based on user info
        AuthLoginRspType userInfo =  (AuthLoginRspType) auth.getPrincipal();
        String token = JwtTokenUtil.createToken(userInfo, null); // current token
        String shadowToken = JwtTokenUtil.createToken(userInfo, "shadow"); // refresh token

        // Add token to response header
        // shadow token is used to expand valid time after auth token expires.
        // in front end the shadow token will be saved into local storage. it can be used when page is refreshed.
        response.addHeader(JwtConfig.authField,  token);
        response.addHeader(JwtConfig.shadowField,  shadowToken);

        // Add json payload into response body
        PrintWriter writer = response.getWriter();
        writer.write(JSONUtil.parseObj(UniformResponse.ok().data(userInfo)).toString());
        writer.flush();
        writer.close();
    }

    /**
     * Fail to authenticate
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        PrintWriter writer = response.getWriter();

        if (failed instanceof UsernameNotFoundException){
            writer.write(JSONUtil.parseObj(UniformResponse.error(USER_NOT_EXIST)).toString());
        }
        else if (failed instanceof LockedException){
            writer.write(JSONUtil.parseObj(UniformResponse.error(USER_IS_FROZEN)).toString());
        }
        else if (failed instanceof BadCredentialsException){
            writer.write(JSONUtil.parseObj(UniformResponse.error(USER_AUTH_FAILURE)).toString());
        }
        else if (failed instanceof DisabledException){
            writer.write(JSONUtil.parseObj(UniformResponse.error(USER_NO_PERMIT)).toString());
        }
        else{
            writer.write(JSONUtil.parseObj(UniformResponse.error(USER_UNKNOWN_IDENTITY)).toString());
        }
    }
}
