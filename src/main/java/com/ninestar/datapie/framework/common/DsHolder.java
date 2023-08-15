package com.ninestar.datapie.framework.common;

import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.ninestar.datapie.datamagic.config.DruidConfig;
import com.ninestar.datapie.datamagic.config.HikariCpConfig;
import com.ninestar.datapie.framework.model.JdbcSource;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class DsHolder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static boolean druidPoolEnable = true;

    // manage all sources with a map pool
    // current Hikari is being used
    private static Map<Integer, HikariDataSource> hikariSourceMap = new HashMap<>();
    private static Map<Integer, DruidDataSource> druidSourceMap = new HashMap<>();
    /*
     * Check if source exists or not
     */
    public boolean isSourceExist(Integer id) {
        if(druidPoolEnable){
            return druidSourceMap.containsKey(id);
        }
        else{
            return hikariSourceMap.containsKey(id);
        }
    }
    public boolean isSourceExist(String name) {
        if(druidPoolEnable){
            return druidSourceMap.values().contains(name);
        }
        else{
            return hikariSourceMap.values().contains(name);
        }

    }

    /*
    * covert common JdbcSource to HikariDataSource and add it to pool for management
    */
    public void addSource(Integer id, String sourceName, String type, String url, String params, String username, String password) {
        if(isSourceExist(id)){ return; }

        String jdbcUrl = "jdbc:" + type.toLowerCase() + "://" + url.trim();
        if(url.endsWith("?")){
            jdbcUrl += params;
        }
        else{
            jdbcUrl += "?" + params;
        }

        try {
            String className = DriverManager.getDriver(jdbcUrl.trim()).getClass().getName();
            if (StrUtil.isEmpty(className) || className.contains("com.sun.proxy")
                    || className.contains("net.sf.cglib.proxy")) {
                className = null;
            }

            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new Exception("Unable to get driver instance for jdbcUrl: " + jdbcUrl);
            }

            // every datasource is a pool

            // Hikari Datasource pool
            HikariDataSource hikariSource = new HikariDataSource();
            hikariSource.setPoolName(sourceName);
            hikariSource.setUsername(username);
            hikariSource.setPassword(password);
            hikariSource.setJdbcUrl(jdbcUrl);
            hikariSource.setDriverClassName(className);
            hikariSource.setMaximumPoolSize(HikariCpConfig.maximumPoolSize);
            hikariSource.setMinimumIdle(HikariCpConfig.minimumIdle);
            hikariSource.setMaxLifetime(HikariCpConfig.maxLifeTime);
            hikariSource.setIdleTimeout(HikariCpConfig.idleTimeout);
            hikariSource.setKeepaliveTime(HikariCpConfig.keepaliveTime);

            // Druid Datasource pool
            DruidDataSource druidSource = new DruidDataSource();
            druidSource.setName(sourceName);
            druidSource.setUrl(jdbcUrl);
            druidSource.setUsername(username);
            druidSource.setPassword(password);
            druidSource.setDriverClassName(className);
            druidSource.setInitialSize(DruidConfig.initialSize);
            druidSource.setMinIdle(DruidConfig.minIdle);
            druidSource.setMaxActive(DruidConfig.maxActive);
            druidSource.setMaxWait(DruidConfig.maxWait);
            druidSource.setTestWhileIdle(DruidConfig.testWhileIdle);
            druidSource.setTestOnBorrow(DruidConfig.testOnBorrow);
            druidSource.setTestOnReturn(DruidConfig.testOnReturn);
            druidSource.setValidationQuery(DruidConfig.validationQuery);

            Properties properties = new Properties();
            properties.setProperty("druid.mysql.usePingMethod", "false");
            druidSource.setConnectProperties(properties);

            // add to map
            if(druidPoolEnable){
                druidSourceMap.put(id, druidSource);
            }
            else{
                hikariSourceMap.put(id, hikariSource);
            }
        }
        catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    /*
     * covert common JdbcSource to HikariDataSource and add it to pool for management
     */
    public void addSource(JdbcSource jdbcSource) {
        Integer id = jdbcSource.getId();
        if(isSourceExist(id)){ return; }

        String jdbcUrl = jdbcSource.getUrl();
        try {
            String className = DriverManager.getDriver(jdbcUrl.trim()).getClass().getName();
            if (StrUtil.isEmpty(className) || className.contains("com.sun.proxy")
                    || className.contains("net.sf.cglib.proxy")) {
                className = null;
            }

            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new Exception("Unable to get driver instance for jdbcUrl: " + jdbcUrl);
            }


            // Hikari Datasource pool
            HikariDataSource hikariSource = new HikariDataSource();
            hikariSource.setPoolName(jdbcSource.getName());
            hikariSource.setUsername(jdbcSource.getUsername());
            hikariSource.setPassword(jdbcSource.getPassword());
            hikariSource.setJdbcUrl(jdbcUrl);
            hikariSource.setDriverClassName(className);
            hikariSource.setMaximumPoolSize(HikariCpConfig.maximumPoolSize);
            hikariSource.setMinimumIdle(HikariCpConfig.minimumIdle);
            hikariSource.setMaxLifetime(HikariCpConfig.maxLifeTime);
            hikariSource.setIdleTimeout(HikariCpConfig.idleTimeout);
            hikariSource.setKeepaliveTime(HikariCpConfig.keepaliveTime);

            // Druid Datasource pool
            DruidDataSource druidSource = new DruidDataSource();
            druidSource.setName(jdbcSource.getName());
            druidSource.setUrl(jdbcUrl);
            druidSource.setUsername(jdbcSource.getUsername());
            druidSource.setPassword(jdbcSource.getPassword());
            druidSource.setDriverClassName(className);
            druidSource.setInitialSize(DruidConfig.initialSize);
            druidSource.setMinIdle(DruidConfig.minIdle);
            druidSource.setMaxActive(DruidConfig.maxActive);
            druidSource.setMaxWait(DruidConfig.maxWait);
            druidSource.setTestWhileIdle(DruidConfig.testWhileIdle);
            druidSource.setTestOnBorrow(DruidConfig.testOnBorrow);
            druidSource.setTestOnReturn(DruidConfig.testOnReturn);
            druidSource.setValidationQuery(DruidConfig.validationQuery);

            Properties properties = new Properties();
            properties.setProperty("druid.mysql.usePingMethod", "false");
            druidSource.setConnectProperties(properties);

            // add to map
            if(druidPoolEnable){
                druidSourceMap.put(id, druidSource);
            }
            else{
                hikariSourceMap.put(id, hikariSource);
            }

        }
        catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    /*
     * remove source from pool
     */
    public void removeSource(Integer id) {
        if(!isSourceExist(id)){ return; }

        try {
            if(druidPoolEnable){
                DruidDataSource druidSource = druidSourceMap.remove(id);
                if (druidSource != null) {
                    druidSource.close();
                }
            }
            else{
                HikariDataSource hikariSource = hikariSourceMap.remove(id);
                if (hikariSource != null) {
                    hikariSource.close();
                }
            }


        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    /*
     * get a source from pool
     */
    public DataSource getSource(Integer id) {
        if (!isSourceExist(id)) { return null; }
        if(druidPoolEnable){
            return druidSourceMap.get(id);
        }
        else{
            return hikariSourceMap.get(id);
        }
    }

    /*
     * get a connection from pool
     */
    public Connection getConnection(Integer id) throws SQLException {
        if (!isSourceExist(id)) { return null; }
        if(druidPoolEnable){
            return druidSourceMap.get(id).getConnection();
        }
        else{
            return hikariSourceMap.get(id).getConnection();
        }
    }
}
