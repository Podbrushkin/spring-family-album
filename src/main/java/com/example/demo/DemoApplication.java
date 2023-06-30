package com.example.demo;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@SpringBootApplication
public class DemoApplication {
	Logger log = LoggerFactory.getLogger(getClass());
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Autowired
	Environment env;

	/* @Bean
	public DataSource dataSource() {
		// System.out.println("------------------------------");
		var className = env.getProperty("jdbc.driverClassName");
		log.info("driverClassName: {}", className);
		final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(env.getProperty("jdbc.driverClassName"));
		dataSource.setUrl(env.getProperty("jdbc.url"));
		dataSource.s
		// dataSource.setUsername(env.getProperty("user"));
		// dataSource.setPassword(env.getProperty("password"));
		log.info("------------------------------dataSource created: {}", dataSource);
		
		return dataSource;
	} */

}
