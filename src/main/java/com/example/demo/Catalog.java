package com.example.demo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.model.Image;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Component
public class Catalog {
    Logger log = LoggerFactory.getLogger(getClass());
    // private Path jsonFile = Path.of("C:\\Users\\user\\tagToHashes 20230531.json");
    // private Path tagToImagesJson = Path.of("C:\\Users\\user\\tagToImages 20230602.json");
    private Path imageObjectsJson = Path.of("C:\\Users\\user\\imageObjects 20230609.json");

    private Path tagIdToNameFile = Path.of("C:\\Users\\user\\Documents\\RelativesDates.txt");
    // private Map<String,TreeSet<String>> tagToHashesMap = readTagToHashesMap(jsonFile);
    // private Map<String,Set<Image>> tagToImagesMap = readTagToImagesMap(tagToImagesJson);
    private Map<String,String> tagIdToNameMap = readTagIdToNameMap(tagIdToNameFile);
    
    
    private Set<Image> imageObjects = readImageObjectsFromJson(imageObjectsJson);
    private Map<String,Image> mgckHashToImageMap = createMgckHashToImageMap();
    private Set<String> tags = getTags();

    private Map<String,Image> createMgckHashToImageMap() {
        return
            getImageObjects().collect(Collectors.toMap(Image::getImageHash, img -> img));
    }
    public Image getImageForMgckHash(String mgckHash) {
        return mgckHashToImageMap.get(mgckHash);
    }

    /* private Map<String,TreeSet<String>> readTagToHashesMap(Path jsonFile) {

        try {
            return
                new ObjectMapper()
                    .readValue(jsonFile.toFile(), new TypeReference<Map<String, TreeSet<String>>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    } */

    public Map<Integer, Long> getNumberOfPhotosByYear() {
        return imageObjects.stream()
                .map(Image::getCreationDate)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(LocalDateTime::getYear, Collectors.counting()));
    }

    public Stream<Image> getImageObjects() {
        return imageObjects.stream();
    }

    // public Map<Integer, Integer> getNumberOfPhotosByYear() {
    // Map<Integer, Integer> yearToImageCountMap = new HashMap<>();
    //     for (Set<Image> images : tagToImagesMap.values()) {
    //         for (Image image : images) {
    //             LocalDateTime creationDate = image.getCreationDate();
    //             if (creationDate != null) {
    //                 int year = creationDate.getYear();
    //                 yearToImageCountMap.put(year, yearToImageCountMap.getOrDefault(year, 0) + 1);
    //             }
    //         }
    //     }
    //     return yearToImageCountMap;
    // }

    private Set<Image> readImageObjectsFromJson(Path imageObjectsJson) {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new SimpleModule().addDeserializer(Image.class, new ImageDeserializer()));
        Set<Image> imageObjects = null;
        try {
            imageObjects =
                objectMapper
                    .readValue(imageObjectsJson.toFile(), new TypeReference<Set<Image>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("Found {} images.", imageObjects.size());
        return imageObjects;
    }

    /* private Map<String,Set<Image>> readTagToImagesMap(Path tagToImagesJson) {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new SimpleModule().addDeserializer(Image.class, new ImageDeserializer()));
        try {
            return
                objectMapper
                    .readValue(tagToImagesJson.toFile(), new TypeReference<Map<String, Set<Image>>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    } */

    public Stream<Image> getImagesForTag(String tag) {
        log.info("Asked for {} tag.",tag);
        // return tagToHashesMap.get(tag).stream().limit(10).collect(Collectors.toSet());
        return
        imageObjects.stream()
            .filter(img -> img.getTags().contains(tag));
        // return
        //     tagToImagesMap.get(tag).stream();
            
    }

    /* public Collection<String> getHashesForTag(String tag) {
        log.info("Asked for {} tag.",tag);
        // return tagToHashesMap.get(tag).stream().limit(10).collect(Collectors.toSet());
        return
        tagToImagesMap.get(tag).stream()
            .sorted(Comparator.comparing(Image::getCreationDate))
            .map(Image::getImageHash)
            .collect(Collectors.toList());

        // return tagToHashesMap.get(tag);
    } */




    public Set<String> getTags() {
        if (tags == null) {
            tags = imageObjects.stream()
            .flatMap(img -> img.getTags().stream())
            .distinct()
            .collect(Collectors.toSet());
            log.info("Found {} unique tags.", tags.size());
        }
        return tags;
            
        // return new ArrayList<String>(tagToImagesMap.keySet());
    }

    public String getTagNameExtended(String tagId) {
        return tagIdToNameMap.get(tagId);
    }
    private Map<String,String> readTagIdToNameMap(Path tagIdToNameFile) {
        var tagIdToNameMap = new HashMap<String,String>();
        
        try {
            Files.lines(tagIdToNameFile,Charset.forName("UTF-8"))
            .skip(1)
            .forEach(s -> {
                var pair = s.split("\\t");
                tagIdToNameMap.put(pair[1], pair[0]);
            });
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return tagIdToNameMap;
    }
    
}
