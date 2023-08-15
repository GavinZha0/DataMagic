
package com.ninestar.datapie.datamagic.config;
/**
 * Druid database pool config
 */

import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.datasource.druid")
public class DruidConfig {

    @Value("${spring.datasource.druid.maxActive}")
    public static int maxActive;

    @Value("${spring.datasource.druid.initial-size}")
    public static int initialSize;

    @Value("${spring.datasource.druid.min-idle}")
    public static int minIdle;

    @Value("${spring.datasource.druid.maxWait}")
    public static int maxWait;

    @Value("${spring.datasource.druid.testWhileIdle}")
    public static boolean testWhileIdle;

    @Value("${spring.datasource.druid.testOnBorrow}")
    public static boolean testOnBorrow;

    @Value("${spring.datasource.druid.testOnReturn}")
    public static boolean testOnReturn;

    @Value("${spring.datasource.druid.validationQuery}")
    public static String validationQuery;

    @Value("${spring.datasource.druid.filters}")
    public static String filters;

    @Value("${spring.datasource.druid.stat-view-servlet.enabled}")
    public static boolean stateViewEnable;

    @Value("${spring.datasource.druid.stat-view-servlet.reset-enable}")
    public static boolean stateViewResetEnable;

    @Value("${spring.datasource.druid.stat-view-servlet.login-username}")
    public static String stateViewServletLoginUsername = "admin";

    @Value("${spring.datasource.druid.stat-view-servlet.login-password}")
    public static String stateViewPassword = "123456";


    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }
    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }
    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }
    public void setMaxWait(int maxWait) {
        this.maxWait = maxWait;
    }
    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }
    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }
    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }
    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }
    public void setFilters(String filters) {
        this.filters = filters;
    }

    public void setStateViewEnable(boolean stateViewEnable) {
        this.stateViewEnable = stateViewEnable;
    }
    public void setStateViewResetEnable(boolean stateViewResetEnable) {
        this.stateViewResetEnable = stateViewResetEnable;
    }
    public void setStateViewServletLoginUsername(String stateViewServletLoginUsername) {
        this.stateViewServletLoginUsername = stateViewServletLoginUsername;
    }
    public void setStateViewPassword(String stateViewPassword) {
        this.stateViewPassword = stateViewPassword;
    }

    @Bean
    public ServletRegistrationBean druidStateViewServlet() {
        ServletRegistrationBean reg = new ServletRegistrationBean();
        reg.setServlet(new StatViewServlet());
        reg.addUrlMappings("/druid/*");
        reg.addInitParameter("allow", "127.0.0.1");
        reg.addInitParameter("loginUsername", stateViewServletLoginUsername);
        reg.addInitParameter("loginPassword", stateViewPassword);
        reg.addInitParameter("resetEnable", "false");
        return reg;
    }

    @Bean
    public FilterRegistrationBean filterRegistrationBean() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new WebStatFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.addInitParameter("exclusions", "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        filterRegistrationBean.addInitParameter("profileEnable", "true");
        filterRegistrationBean.addInitParameter("principalCookieName", "USER_COOKIE");
        filterRegistrationBean.addInitParameter("principalSessionName", "USER_SESSION");
        return filterRegistrationBean;
    }
}
