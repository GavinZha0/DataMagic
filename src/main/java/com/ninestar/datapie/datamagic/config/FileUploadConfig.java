package com.ninestar.datapie.datamagic.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import javax.servlet.MultipartConfigElement;

@Configuration
public class FileUploadConfig {
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // single file size (M)
        factory.setMaxFileSize(DataSize.ofMegabytes(200L));
        // total size of uploading (G)
        factory.setMaxRequestSize(DataSize.ofGigabytes(10L));

        return factory.createMultipartConfig();
    }
}
