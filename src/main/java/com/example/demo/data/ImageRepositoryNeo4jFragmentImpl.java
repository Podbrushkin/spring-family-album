package com.example.demo.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.neo4j.driver.Driver;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.QueryExecutionType;
import org.neo4j.graphdb.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jTemplate;

import com.example.demo.model.Image;

public class ImageRepositoryNeo4jFragmentImpl implements ImageRepositoryNeo4jFragment {
    private Logger log = LoggerFactory.getLogger(getClass());
    private Neo4jTemplate neo4jTemplate;
    private GraphDatabaseService graphDatabaseService;
    private Driver driver;

    ImageRepositoryNeo4jFragmentImpl(
        Neo4jTemplate neo4jTemplate,
        GraphDatabaseService graphDatabaseService,
        Driver driver) {

        this.neo4jTemplate = neo4jTemplate;
        this.graphDatabaseService = graphDatabaseService;
        this.driver = driver;
    }

    @Override
    public List<Image> findImagesByCypherQuery(String cypherQuery) {
        return neo4jTemplate.findAll(cypherQuery, Image.class);
    }

    public String executeReadAndGetResultAsString(String cypherQuery) {
        String resultString = "";
        try (var tx = graphDatabaseService.beginTx()) {
            
            Result result = tx.execute(cypherQuery);
            var qExType = result.getQueryExecutionType();
            if (qExType.queryType().equals(QueryExecutionType.QueryType.READ_ONLY)) {
                log.debug("QueryExecutionType.queryType() is ok: "+qExType.queryType());
                // resultString = result.resultAsString();

                var resultStrings = new ArrayList<String>();
                resultStrings.add(String.join("\t", result.columns()));
                
                List<String> rowLines = result.stream()
                    .map(row -> result.columns().stream()
                        .map(column -> String.valueOf(row.get(column)))
                        .collect(Collectors.joining("\t")))
                    .collect(Collectors.toList());

                resultStrings.addAll(rowLines);
                resultString = String.join("\n", resultStrings);
                
                tx.commit();
            }
                
        }
        return resultString;
    }
    
    /* public String executeReadAndGetResultAsString(String cypherQuery) {
        try (var session = driver.session()) {
            var resultString = session.executeRead(tx -> {
                var result = tx.run(cypherQuery);
                return result.list(r -> {
                    return r.asMap().toString();
                });
            });
            return resultString.toString();
        }
    } */
}
