package com.example.demo.data;

import java.util.Set;

import com.example.demo.model.Image;

public interface ImageRepository {

    Set<Image> getImages();

}