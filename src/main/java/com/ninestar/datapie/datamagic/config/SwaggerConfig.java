package com.ninestar.datapie.datamagic.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import javax.annotation.Resource;

@EnableKnife4j
@Configuration
public class SwaggerConfig {

    @Value("${knife4j.production}")
    private Boolean enable;

    //expend knife4j
    private OpenApiExtensionResolver openApiExtensionResolver;

    @Resource
    public void SwaggerConfiguration(OpenApiExtensionResolver openApiExtensionResolver) {
        this.openApiExtensionResolver = openApiExtensionResolver;
    }

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .enable(!enable)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))// Annotation to generate doc
                .paths(PathSelectors.any())
                .build()
                .extensions(openApiExtensionResolver.buildExtensions("Latest"));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Interface of DataMagic")
                .description("DataMagic is a Java server of DataPie for AI and BI!")
                .contact(new Contact("NineStar", "https://www.ninestar.com/", "support@ninestar.com"))
                .version("0.1")
                .build();
    }
}
