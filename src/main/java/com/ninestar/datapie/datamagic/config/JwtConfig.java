package com.ninestar.datapie.datamagic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT config
 * @author Gavin.Zhao
 * This is static config so members can't get config from *.properties automatically
 * That is why the set methods are there
**/

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    public static String secretKey;
    public static String authField;
    public static String shadowField;
    public static String tokenPrefix;
    public static Integer expiration; //ms

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setAuthField(String authField) {
        this.authField = authField;
    }

    public void setShadowField(String shadowField) {
        this.shadowField = shadowField;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    public void setExpiration(Integer expiration) {
        // from min to ms
        this.expiration = expiration * 60 * 1000;
    }
}
