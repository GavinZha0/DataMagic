package com.ninestar.datapie.datamagic.config;

import com.ninestar.datapie.datamagic.annotation.CustomMethodArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Resource
    private CustomMethodArgumentResolver paramsResolver;
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(paramsResolver);
        //WebMvcConfigurer.super.addArgumentResolvers(resolvers);
    }
}
