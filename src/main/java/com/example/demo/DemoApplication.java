package com.example.demo;

import java.io.File;
import java.time.Duration;

import javax.sql.DataSource;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

@SpringBootApplication
@EnableNeo4jRepositories
public class DemoApplication {
	Logger log = LoggerFactory.getLogger(getClass());
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public Configuration cypherDslConfiguration() {
		return Configuration.newConfig().withDialect(Dialect.NEO4J_5).build();
	}

	@Bean
    public DatabaseManagementService databaseManagementService() {
        DatabaseManagementService managementService = new 
			DatabaseManagementServiceBuilder(new File("target/mydb").toPath())
				.setConfig(GraphDatabaseSettings.transaction_timeout, Duration.ofSeconds( 60 ) )
				.setConfig( BoltConnector.enabled, true )
				.setConfig( BoltConnector.listen_address, 
					new SocketAddress( "localhost", 7687 ) 
					)
        		.build();
		
        return managementService;
    }

    @Bean
    public GraphDatabaseService graphDatabaseService(DatabaseManagementService managementService) {
        // managementService.createDatabase("mydb");
        GraphDatabaseService graphDb =
			managementService.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);
        log.info("Neo4j database Embedded instance is available: {}", graphDb.isAvailable());
        return graphDb;
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
