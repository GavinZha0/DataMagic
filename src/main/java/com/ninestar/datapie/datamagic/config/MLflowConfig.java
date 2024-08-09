package com.ninestar.datapie.datamagic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
public class MLflowConfig {
    @Value("${datasource.mlflow.id}")
    private Integer id;

    @Value("${datasource.mlflow.name}")
    private String name;

    @Value("${datasource.mlflow.type}")
    private String type;

    @Value("${datasource.mlflow.url}")
    private String url;

    @Value("${datasource.mlflow.username}")
    private String username;

    @Value("${datasource.mlflow.password}")
    private String password;

    @Value("${datasource.mlflow.params}")
    private String params;
}
