package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo.model.Image;

import jakarta.annotation.PostConstruct;

@Component
public class ImageProvider {
    Logger log = LoggerFactory.getLogger(ImageProvider.class);

    @Autowired
    private Catalog catalog;
    private Map<String, Path> hashToPath; // = catalog.getImHashToPathMap();
    private Map<String, Path> dgkmHashToThumbPath; // = catalog.getDgkmHashToThumbPathMap();

    @PostConstruct
    
    private void init() {
        hashToPath = catalog.getImHashToPathMap();
        dgkmHashToThumbPath = catalog.getDgkmHashToThumbPathMap();

    }

    public InputStream getImage(String imgHash) {
        File imageFile = hashToPath.get(imgHash).toFile();
        try (InputStream is = new FileInputStream(imageFile)) {
            return is;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getImageBytes(String imgHash) {
        log.trace("Asked image for imghash=" + imgHash);

        File imageFile = hashToPath.get(imgHash).toFile();
        try (InputStream is = new FileInputStream(imageFile)) {
            return is.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getThumbnailBytes(Image image) {
        // Path imageFile = dgkmHashToThumbPath.get(image.getDgkmHash());
        Path thumbFile = image.getThumbPath();

        if (thumbFile != null) {
            log.trace("Found thumbnail for {}", image.getImHash());
            try (InputStream is = new FileInputStream(thumbFile.toFile())) {
                return is.readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return getImageBytes(image.getImHash());
        }
        return null;
    }
}
