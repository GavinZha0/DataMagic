package com.ninestar.datapie.datamagic.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerConfig {
    /*
    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public MeterRegistryCustomizer configurer(MeterRegistry meterRegistry) {
        // custom tags
        return registry -> meterRegistry
                .config()
                .commonTags("app", applicationName);
    }

     */
}
