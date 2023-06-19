package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.model.Image;

import jakarta.annotation.PostConstruct;

@Component
public class ImageProvider {
    // Path tsvFile = Path.of("E:\\PHOTOS\\Очень старые фотографии\\очСтарImgHash 20230414.tsv");
    private Map<String,Path> hashToPath = new HashMap<>();
    private Map<String,Path> dgkmHashToThumbPath = new HashMap<>();
    
    Logger log = LoggerFactory.getLogger(ImageProvider.class);

    @PostConstruct
    public void init() {
        Set<Path> tsvFiles = Set.of(
            Path.of("E:\\PHOTOS\\Очень старые фотографии\\очСтарImgHash 20230609.tsv"),
            Path.of("E:\\PHOTOS\\оцифрПлjpghash20230530.tsv"),
            Path.of("E:\\PHOTOS\\oldphjpghashlen20230211.tsv")
        );
        for (var tsvFile : tsvFiles) {
            hashToPath.putAll(tsvToMap(tsvFile));
        }
        var thumbsDir = Path.of("C:\\users\\user\\thumbs\\");
        Pattern pattern = Pattern.compile("([\\d\\w]{32})\\.(png|jpg)");



        try (DirectoryStream<Path> stream = Files.newDirectoryStream(thumbsDir, "*.{png,jpg}")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                Matcher matcher = pattern.matcher(fileName);
                if (matcher.matches()) {
                    String hash = matcher.group(1);
                    dgkmHashToThumbPath.put(hash, path);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to get thumbnail file paths:", e);
        }

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
        log.info("Asked image for imghash="+imgHash);

        File imageFile = hashToPath.get(imgHash).toFile();
        try (InputStream is = new FileInputStream(imageFile)) {
            return is.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
   
    public byte[] getThumbnailBytes(Image image) {
        Path imageFile = dgkmHashToThumbPath.get(image.getDgkmHash());
        if (imageFile != null) {
            log.info("Found thumbnail for {}", image.getImageHash());
            try (InputStream is = new FileInputStream(imageFile.toFile())) {
                return is.readAllBytes();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            return getImageBytes(image.getImageHash());
        }
        return null;
    }

    private Map<String,Path> tsvToMap(Path tsvFile) {
        
        var hashToPath = new HashMap<String,Path>();

        try {
            Files.lines(tsvFile)
                .forEach((line -> {
                    String str[] = line.split("\t");
                    hashToPath.put(str[1], Path.of(str[0]));
                }));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hashToPath;
    }
    
}
