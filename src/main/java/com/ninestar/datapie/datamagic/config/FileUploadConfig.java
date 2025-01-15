package com.ninestar.datapie.datamagic.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.beans.factory.annotation.Value;
import javax.servlet.MultipartConfigElement;
import java.io.File;

@Configuration
public class FileUploadConfig {
    @Value("${spring.servlet.multipart.location}")
    private String location;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // single file size (M)
        factory.setMaxFileSize(DataSize.ofMegabytes(200L));
        // total size of uploading (G)
        factory.setMaxRequestSize(DataSize.ofGigabytes(10L));

        // set the path of tmp
        String uploadDir = System.getProperty("user.dir") + location;
        File file = new File(uploadDir);
        if(!file.exists()){
            file.mkdirs();
        }
        factory.setLocation(uploadDir);
        return factory.createMultipartConfig();
    }
}
