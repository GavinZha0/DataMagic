package com.ninestar.datapie.datamagic.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MicrometerConfig {
    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public MeterRegistryCustomizer configurer(MeterRegistry meterRegistry) {
        // custom tags
        return registry -> meterRegistry
                .config()
                .commonTags("app", applicationName);
    }
}
