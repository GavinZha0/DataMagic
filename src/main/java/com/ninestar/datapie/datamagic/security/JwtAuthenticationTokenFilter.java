package com.ninestar.datapie.datamagic.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ninestar.datapie.datamagic.bridge.AuthLoginRspType;
import com.ninestar.datapie.datamagic.config.JwtConfig;
import com.ninestar.datapie.datamagic.utils.JwtTokenUtil;
import com.ninestar.datapie.framework.utils.UniformResponse;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static com.ninestar.datapie.framework.consts.UniformResponseCode.*;

/**
 * @author Gavin.Zhao
 * Intercept http request and verify jwt token
**/
@Slf4j
public class JwtAuthenticationTokenFilter extends BasicAuthenticationFilter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public JwtAuthenticationTokenFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Get jwt token from request header
        String token = request.getHeader(JwtConfig.authField);
        String shadowToken = request.getHeader(JwtConfig.shadowField);
        String swaggerToken = request.getHeader("Authorization");

        if (!StrUtil.isEmpty(swaggerToken) && swaggerToken.equals("Basic YWRtaW46MTIzNDU2")) {
            // Let it go if it is in white list, like swagger
            filterChain.doFilter(request, response);
            return;
        }

        if (StrUtil.isEmpty(token) || !token.startsWith(JwtConfig.tokenPrefix)) {
            // no jwt token or token is invalid
            PrintWriter writer = response.getWriter();
            writer.write(JSONUtil.parseObj(UniformResponse.error(USER_UNKNOWN_IDENTITY)).toString());
            return;
        }

        try {
            // has jwt token then verify if it expires
            // if so, an exception will be thrown
            JwtTokenUtil.isTokenExpired(token);

            // get user info and role from jwt token
            AuthLoginRspType userInfo = JwtTokenUtil.getUserInfo(token);
            Set<GrantedAuthority> authorities = new HashSet<>();
            for(String role: userInfo.roleName){
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }

            if(false){
                //verify user permission
                // return if current user doesn't have permission to visit target resource(url)
                // for example, only admin has permission to visit control panel
                PrintWriter writer = response.getWriter();
                writer.write(JSONUtil.parseObj(UniformResponse.error(USER_NO_PERMIT)).toString());
            }

            // build auth token and pass it to following steps (I put user NAME into credentials field)
            // user id, name and org can be used to filter resource when get data from DB
            // client is not necessary to bring user info when send request to backend because we have these in token
            // but if so, swagger will not work well because there is not org. So swagger should provide org when login.  -- to do / Gavin
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userInfo.id, userInfo.name, authorities);
            authentication.setDetails(userInfo.orgId);

            // put necessary info into security context
            // it can be used by controllers in following steps in this thread only
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // get new shadow token if current token is equal shadow token
            // it happens when you refresh browser with CTRL+F5 （SHIFT+COMMAND+R）
            if(token.equals(shadowToken)){
                String newShadowToken = JwtTokenUtil.createToken(userInfo, "shadow"); // refresh token
                response.addHeader(JwtConfig.authField,  token);
                response.addHeader(JwtConfig.shadowField,  newShadowToken);
            }
        } catch (ExpiredJwtException e){
            try {
                // auth token expired, so verify the shadow token which has double expiration time
                JwtTokenUtil.isTokenExpired(shadowToken);
            }catch (ExpiredJwtException ex){
                // return failure if both tokens expire
                logger.error("Auth token expired!");
                PrintWriter writer = response.getWriter();
                writer.write(JSONUtil.parseObj(UniformResponse.error(TOKEN_EXPIRED)).toString());
                return;
            }

            // replace auth token with shadow token for keeping communication
            // and generate a new shadow token
            // front end should update tokens and save them when receive this response
            AuthLoginRspType userInfo = JwtTokenUtil.getUserInfo(shadowToken);
            String newShadowToken = JwtTokenUtil.createToken(userInfo, "shadow"); // refresh token
            response.addHeader(JwtConfig.authField,  shadowToken);
            response.addHeader(JwtConfig.shadowField,  newShadowToken);
        } catch (MissingClaimException e) {
            logger.error("Claim is missed from token!");
            PrintWriter writer = response.getWriter();
            writer.write(JSONUtil.parseObj(UniformResponse.error(TOKEN_VERIFICATION_FAIL)).toString());
            return;

        } catch (MalformedJwtException e) {
            logger.error("Malformed token!");
            PrintWriter writer = response.getWriter();
            writer.write(JSONUtil.parseObj(UniformResponse.error(TOKEN_INVALID)).toString());
            return;

        } catch (Exception e) {
            logger.error("Auth token exception!", e);
            PrintWriter writer = response.getWriter();
            writer.write(JSONUtil.parseObj(UniformResponse.error(TOKEN_INVALID)).toString());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
