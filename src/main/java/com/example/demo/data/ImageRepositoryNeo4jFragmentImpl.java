package com.example.demo.data;

import java.util.List;

import org.springframework.data.neo4j.core.Neo4jTemplate;

import com.example.demo.model.Image;

public class ImageRepositoryNeo4jFragmentImpl implements ImageRepositoryNeo4jFragment {
    
    Neo4jTemplate neo4jTemplate;

    ImageRepositoryNeo4jFragmentImpl(Neo4jTemplate neo4jTemplate) {
        this.neo4jTemplate = neo4jTemplate;
    }

    @Override
    public List<Image> findImagesByCypherQuery(String cypherQuery) {
        return neo4jTemplate.findAll(cypherQuery, Image.class);
    }
}
