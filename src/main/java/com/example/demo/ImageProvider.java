package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
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

    private Map<String, byte[]> imageDataCache = new LinkedHashMap<String, byte[]>(16, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > 200;
        }
    };

    @PostConstruct
    private void init() {
        hashToPath = catalog.getImHashToPathMap();
        // dgkmHashToThumbPath = catalog.getDgkmHashToThumbPathMap();

    }

    synchronized public byte[] getImageBytes(String imgHash) {
        log.trace("Asked image for imghash=" + imgHash);
        var img = catalog.getImageForHash(imgHash);
        return imageDataCache.computeIfAbsent(imgHash, hash -> {
            // File imageFile = hashToPath.get(hash).toFile();
            File imageFile = img.getFilePath().toFile();

            try (InputStream is = new FileInputStream(imageFile)) {
                log.trace("reading from disk...");
                return is.readAllBytes();

            } catch (IOException e) {
                log.warn("Failed to read image from disk: ", e);
            }
            return null;
        });
    }

    public byte[] getImageBytes(Image img) {
        return img.getDgkmHash() == null ? getImageBytes(img.getImHash()) : getImageBytes(img.getDgkmHash());
    }

    public byte[] getThumbnailBytes(Image image) {
        // Path imageFile = dgkmHashToThumbPath.get(image.getDgkmHash());
        Path thumbFile = image.getThumbPath();

        if (thumbFile != null) {
            log.trace("Found thumbnail for {}", image);
            try (InputStream is = new FileInputStream(thumbFile.toFile())) {
                return is.readAllBytes();
            } catch (IOException e) {
                log.warn("Failed to read thumbnail: {}", thumbFile, e);
            }
        } else {
            return getImageBytes(image);
        }
        return null;
    }
}
