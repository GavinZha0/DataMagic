package com.ninestar.datapie.datamagic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
//@EnableWebSocket
@EnableAsync
@EnableGlobalMethodSecurity(prePostEnabled=true)
@EnableJpaRepositories("com.ninestar.datapie.datamagic.repository")
public class DataMagicApplication {

	public static void main(String[] args) throws Throwable {
		// set UTC as default time zone of JVM - Gavin
		TimeZone.setDefault(TimeZone.getTimeZone( "UTC"));
		SpringApplication.run(DataMagicApplication.class, args);
	}

}
