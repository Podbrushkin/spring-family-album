package com.example.demo;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {
	Logger log = LoggerFactory.getLogger(getClass());
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
	
	@Bean
	public DataSource dataSource(
		@Value("${jdbc.driverClassName:#{null}}") String driverClassName,
		@Value("${jdbc.url:#{null}}") String jdbcUrl
	) {
		if ((driverClassName != null) && 
			(jdbcUrl != null)) {

			return DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(jdbcUrl)
                .build();
		} else {
			// It isn't used anywhere but Spring Boot needs it
			return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url("jdbc:sqlite:memory:mockDb")
                .build();
		}
	}

}
