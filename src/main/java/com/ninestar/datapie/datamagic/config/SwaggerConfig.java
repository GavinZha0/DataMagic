package com.ninestar.datapie.datamagic.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.ExternalDocumentation;

@EnableKnife4j
@Configuration
public class SwaggerConfig {

    @Value("${knife4j.production}")
    private Boolean enable;

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("DataMagic API")
                        .description("DataPie Rest API")
                        .version("v1.0")
                        .contact(new Contact().name("NineStar Tech").url("https://github.com/GavinZha0/DataMagic")))
                .externalDocs(new ExternalDocumentation()
                        .description("DataPie: A lowcode data framework for AI and BI!")
                        .url("https://datapie.com"));
    }
}
