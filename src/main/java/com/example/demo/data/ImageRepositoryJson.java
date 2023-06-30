package com.example.demo.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Image;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Repository
// @Lazy
public class ImageRepositoryJson implements ImageRepository {
    Logger log = LoggerFactory.getLogger(getClass());
    Set<Image> images;
    ImageDeserializer imageDeserializer;
    Path imageObjectsJson;

    ImageRepositoryJson(
        @Value("${filepaths.imageObjectsJson}") String imageObjectsJsonStr,
        ImageDeserializer imageDeserializer
    ) {
        imageObjectsJson = Path.of(imageObjectsJsonStr);
        this.imageDeserializer = imageDeserializer;
    }

    private Set<Image> readImageObjectsFromJson(Path imageObjectsJson) {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new SimpleModule().addDeserializer(Image.class, imageDeserializer));
        Set<Image> imageObjects = null;
        try {
            imageObjects = objectMapper
                    .readValue(imageObjectsJson.toFile(), new TypeReference<Set<Image>>() {
                    });
        } catch (IOException e) {
            log.error("Failed to read json: ",e);
        }
        log.info("Found {} images.", imageObjects.size());
        return imageObjects;
    }

    @Override
    public Set<Image> getImages() {
        if (images == null) {
            synchronized (this) {
                if (images == null) {
                    images = readImageObjectsFromJson(imageObjectsJson);
                }
            }
        }
        return images;
    }
    
}
