package com.example.demo;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.exceptions.KernelException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.api.procedure.GlobalProcedures;
import org.neo4j.kernel.internal.GraphDatabaseAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootApplication
@EnableNeo4jRepositories
public class DemoApplication {
	private static Logger log = LoggerFactory.getLogger(DemoApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public Configuration cypherDslConfiguration() {
		return Configuration.newConfig().withDialect(Dialect.NEO4J_5).build();
	}

	//https://neo4j.com/docs/java-reference/current/java-embedded/setup/
	@Bean
    public DatabaseManagementService databaseManagementService() {
        DatabaseManagementService managementService = new 
			DatabaseManagementServiceBuilder(Path.of("target","mydb"))
				.setConfig(GraphDatabaseSettings.transaction_timeout, Duration.ofSeconds( 60 ) )
				.setConfig( BoltConnector.enabled, true )
				// .setConfig( BoltConnector.listen_address, new SocketAddress( "localhost", 7687 ) )
				.setConfig(BoltConnector.encryption_level, BoltConnector.EncryptionLevel.DISABLED)
				.setConfig(GraphDatabaseSettings.procedure_unrestricted, List.of("apoc.*"))
				// .setConfig(HttpConnector.enabled, true)
				// .setConfig(HttpConnector.listen_address, new SocketAddress("localhost", 7474))
				.build();
		
		registerShutdownHook(managementService);
        return managementService;
    }

    @Bean
    public GraphDatabaseService graphDatabaseService(DatabaseManagementService managementService) {
        // managementService.createDatabase("mydb");
        GraphDatabaseService graphDb =
			managementService.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);
        log.info("Neo4j database Embedded instance is available: {}", graphDb.isAvailable());
		registerProcedure(graphDb,
                // apoc.coll.Coll.class,
                // apoc.map.Maps.class,
                // apoc.convert.Json.class,
                // Create.class,
                // apoc.date.Date.class,
                // apoc.lock.Lock.class,
                // apoc.load.LoadJson.class,
                // // LoadCsv.class,
                // apoc.load.Xml.class,
                // apoc.path.PathExplorer.class,
                // Meta.class,
                // apoc.refactor.GraphRefactoring.class,
                //apoc.periodic.Periodic.class
				apoc.export.json.ExportJson.class,
				apoc.help.Help.class);
        return graphDb;
    }

	private static void registerShutdownHook(final DatabaseManagementService managementService) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                managementService.shutdown();
            }
        });
    }

	public static void registerProcedure(GraphDatabaseService db, Class<?>... procedures) {
		log.debug("Registering procedures...");
        GlobalProcedures globalProcedures = ((GraphDatabaseAPI) db).getDependencyResolver().resolveDependency(GlobalProcedures.class);
        for (Class<?> procedure : procedures) {
            try {
                globalProcedures.registerProcedure(procedure);
                globalProcedures.registerFunction(procedure);
                globalProcedures.registerAggregationFunction(procedure);
            } catch (KernelException e) {
                throw new RuntimeException("while registering " + procedure, e);
            }
        }
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

	// https://www.baeldung.com/spring-boot-get-all-endpoints
	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) {
		ApplicationContext applicationContext = event.getApplicationContext();
		RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext
			.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
		Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping
			.getHandlerMethods();

		var endpoints = new StringBuilder();
		map.forEach((key, value) -> 
			endpoints.append(key+"\t"+value+"\n")
			// log.info("{} {}", key, value)
			);
		log.info("Following endpoints have been found: \n {}", endpoints);
	}

	

}
