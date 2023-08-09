package com.example.demo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.data.ImageRepositoryNeo4j;
import com.example.demo.model.Image;

@RestController
@RequestMapping("/images")
public class ImageController {
    Logger log = LoggerFactory.getLogger(getClass());

    ImageRepositoryNeo4j imageRepository;
    public ImageController(ImageRepositoryNeo4j imageRepository) {
        this.imageRepository = imageRepository;
    }

    
    @GetMapping("/paths")
    List<String> getimPaths() {
        var images = imageRepository.findAll();
        List<String> paths = images.stream()
            .map(Image::getFilePath)
            .map(Object::toString)
            .collect(Collectors.toList());
        return paths;
    }
}