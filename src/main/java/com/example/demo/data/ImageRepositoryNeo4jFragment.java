package com.example.demo.data;

import java.util.List;

import com.example.demo.model.Image;

public interface ImageRepositoryNeo4jFragment {
    
    List<Image> findImagesByCypherQuery(String cypherQuery);
}
