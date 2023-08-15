package com.ninestar.datapie.datamagic.utils;

import cn.hutool.core.util.StrUtil;
import com.ninestar.datapie.datamagic.bridge.AuthLoginRspType;
import com.ninestar.datapie.datamagic.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
import java.util.*;

/**
 * @author Gavin.Zhao
 * date 2021-09-10 10:15
 * util tool for jwt token generation
 */

public class JwtTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
    private static final String CLAIM_KEY_ROLE_ID = "rid";
    private static final String CLAIM_KEY_ROLE_NAME = "role";

    /**
     * Create token based on user info
     */
    public static String createToken(AuthLoginRspType userDetails, String type) {
        Integer times = 1;
        if(!StrUtil.isEmpty(type) && type.equalsIgnoreCase("shadow")){
            times = 2;
        }

        byte[] secretByte = Decoders.BASE64.decode(JwtConfig.secretKey);
        Key signKey = Keys.hmacShaKeyFor(secretByte);

        // must set claims first, otherwise all pre-defined fields(subject, issuer...) will disappear
        // all fields(claims) can be parsed by public method, like https://jwt.io/
        // so don't put sensitive info into token, like password
        // encrypt it then put into token once you really want to do so
        JwtBuilder builder = Jwts.builder()
                .claim(CLAIM_KEY_ROLE_ID, userDetails.roleId)
                .claim(CLAIM_KEY_ROLE_NAME, userDetails.roleName)
                .setId(userDetails.id.toString())
                .setSubject(userDetails.name)
                .setIssuer(userDetails.orgId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JwtConfig.expiration * times))
                .signWith(signKey, SignatureAlgorithm.HS256);

        return JwtConfig.tokenPrefix + builder.compact();
    }

    /**
     * Refresh token to expend expired date
     * @param token       token from client
     */
    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return JwtConfig.tokenPrefix + Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + JwtConfig.expiration))
                .signWith(SignatureAlgorithm.HS512, JwtConfig.secretKey)
                .compact();
    }

    /**
     * Get user id form token
     * @param token       token from client
     */
    public static AuthLoginRspType getUserInfo(String token) {
        Claims claims = getClaimsFromToken(token);
        AuthLoginRspType userInfo = new AuthLoginRspType();
        userInfo.id = Integer.parseInt(claims.getId());
        userInfo.name = claims.getSubject();
        userInfo.orgId = Integer.parseInt(claims.getIssuer());
        userInfo.roleId = claims.get(CLAIM_KEY_ROLE_ID, List.class);
        userInfo.roleName = claims.get(CLAIM_KEY_ROLE_NAME, List.class);
        return userInfo;
    }

    /**
     * Get user id form token
     * @param token       token from client
     */
    public static String getUserId(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getId();
    }

    /**
     * Get username form token
     * @param token       token from client
     */
    public static String getUsername(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Get org form token
     * @param token       token from client
     */
    public static String getOrganization(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getIssuer();
    }

    /**
     * Get issue time form token
     * @param token       token from client
     */
    public static Date getIssueAt(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getIssuedAt();
    }

    /**
     * Get expired date from token
     * @param token       token from client
     */
    private static Date getExpiration(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Get role id from token
     * @param token       token from client
     */
    public static String getRoleId(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get(CLAIM_KEY_ROLE_ID).toString();
    }

    /**
     * Get role name from token
     * @param token       token from client
     */
    public static String getRoleName(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get(CLAIM_KEY_ROLE_NAME).toString();
    }

    /**
     * Get username form token
     * @param token       token from client
     */
    public static String getAuthority(String token) {
        String authority = null;
        try {
            Claims claims = getClaimsFromToken(token);
            authority = claims.get("CLAIM_KEY_AUTH").toString();
        } catch (Exception e) {
            logger.error("Exception when get authority: ", e);
        }
        return authority;
    }

    /**
     * Get claims from token
     * @param token       token from client
     */
    private static Claims getClaimsFromToken(String token) {
        Claims claims = null;
        String realToken = token.replace(JwtConfig.tokenPrefix, "");
        {
            claims = Jwts.parserBuilder()
                    .setAllowedClockSkewSeconds(30L)
                    .setSigningKey(Decoders.BASE64.decode(JwtConfig.secretKey))
                    .build()
                    .parseClaimsJws(realToken)
                    .getBody();
        }
        // an exception will be thrown here when token expires
        return claims;
    }

    /**
     * Validate token
     *
     * @param token       token from client
     * @param username    username from db
     */
    public static boolean isTokenValid(String token, String username) {
        String uName = getUsername(token);
        return uName.equals(username) && !isTokenExpired(token);
    }

    /**
     * token expired or not
     */
    public static boolean isTokenExpired(String token) {
        Date expiredDate = getExpiration(token);
        return expiredDate.before(new Date());
    }
}
