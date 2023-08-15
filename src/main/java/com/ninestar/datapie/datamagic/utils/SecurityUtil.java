package com.ninestar.datapie.datamagic.utils;

import com.ninestar.datapie.datamagic.entity.SysUserEntity;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security工具类
 * @author zwq
 * @date 2020-04-04
**/
public class SecurityUtil {

    private SecurityUtil(){}

    /**
     * Get current user
     */
    public static SysUserEntity getUserInfo(){
        SysUserEntity userDetails = (SysUserEntity) SecurityContextHolder.getContext().getAuthentication() .getPrincipal();
        return userDetails;
    }
    /**
     * Get user id
     */
    public static Integer getUserId(){
        return getUserInfo().getId();
    }
    /**
     * get user name
     */
    public static String getUserName(){
        return getUserInfo().getName();
    }
}
