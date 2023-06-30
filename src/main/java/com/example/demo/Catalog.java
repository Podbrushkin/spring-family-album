package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.example.demo.data.ImageRepository;
import com.example.demo.model.Image;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Component
public class Catalog {
    Logger log = LoggerFactory.getLogger(getClass());
    private Path tagIdToNameFile;
    private Map<String, String> tagIdToNameMap;
    private Set<Image> imageObjects;
    private Map<String, Image> mgckHashToImageMap;
    private Map<String, Image> dgkmHashToImageMap;
    private Set<String> tags;
    private Map<String, Path> imHashToPath;
    private Map<String, Path> dgkmHashToThumbPath;

    public Catalog(
            ImageRepository imageRepository,
            @Value("${filepaths.tagIdToNameFile}") String tagIdToNameFileStr,
            @Value("${filepaths.imageMagickHashFiles}") String[] imHashFiles,
            @Value("${filepaths.thumbsDirectory}") String thumbsDirectory,
            @Value("${filepaths.whiteListDirectories}") String[] whiteListDirectories,
            @Value("${filepaths.blackListDirectories}") String[] blackListDirectories) {
        imageObjects = imageRepository.getImages();
        this.tagIdToNameFile = Path.of(tagIdToNameFileStr);
        tagIdToNameMap = readTagIdToNameMap(tagIdToNameFile);
        // imageObjects = readImageObjectsFromJson(imageObjectsJson);
        mgckHashToImageMap = createMgckHashToImageMap();
        dgkmHashToImageMap = createDgkmHashToImageMap();
        tags = getTags();

        this.imHashToPath = initHashToFilepath(imHashFiles);
        this.imHashToPath.putAll(initDgkmHashToFilepath(imageObjects));
        this.dgkmHashToThumbPath = initDgkmHashToThumb(Path.of(thumbsDirectory));
        fillThumbPathFields(imageObjects, dgkmHashToThumbPath);

        var whiteListDirs = Stream.of(whiteListDirectories).map(s -> Path.of(s)).collect(Collectors.toSet());
        var blackListDirs = Stream.of(blackListDirectories).map(s -> Path.of(s)).collect(Collectors.toSet());
        imageObjects = applyFilters(imageObjects, whiteListDirs, blackListDirs);
    }

    private Set<Image> applyFilters(Collection<Image> imageObjects, Set<Path> whiteListDirs, Set<Path> blackListDirs) {
        int before = imageObjects.size();
        logExtensionCounts(imageObjects);
        int[] arr = new int[3];
        Set<Image> result = imageObjects.stream()
                .filter(img -> {
                    var imgPath = img.getFilePath().toAbsolutePath();
                    if (!imgPath.getFileName().toString().toLowerCase().endsWith(".jpg")) {
                        arr[0]++;
                        return false;
                    }
                    // var exists = Files.exists(imgPath);
                    var exists = true;
                    boolean allowedByWhiteListedDirs = whiteListDirs.isEmpty()
                            ? true
                            : whiteListDirs.stream()
                                    .anyMatch(p -> imgPath.startsWith(p));
                    boolean allowedByBlackListedDirs = blackListDirs.stream()
                            .allMatch(p -> !imgPath.startsWith(p));
                    return exists && allowedByWhiteListedDirs && allowedByBlackListedDirs;
                })
                .collect(Collectors.toSet());
        int after = result.size();
        log.debug("Images dismissed by extension: {}", arr[0]);
        
        String msg = "By whiteListDirs({}) and blackListDirs({}) amount of images changed from {} to {}, by {}.";
        log.debug(msg, whiteListDirs.size(), blackListDirs.size(), before, after, after - before);
        logExtensionCounts(imageObjects);
        return result;
    }

    public void logExtensionCounts(Collection<Image> images) {
        Map<String, Long> extensionCounts = images.stream()
                .map(image -> getFileExtension(image.getFilePath()))
                .collect(Collectors.groupingBy(extension -> extension, Collectors.counting()));
        log.info("Extension counts: {}", extensionCounts);
    }

    private String getFileExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    private Map<String, Path> initHashToFilepath(String[] imHashFiles) {
        log.trace("Найдено {} paths to files with imageMagick hashes.", imHashFiles.length);
        Map<String, Path> hashToPath = new HashMap<>();
        Set<Path> tsvFiles = new HashSet<Path>();

        for (var filePath : imHashFiles) {
            var exists = new File(filePath).exists();
            log.trace("{} exists: {}", filePath, exists);
            if (exists) {
                tsvFiles.add(Path.of(filePath));
            }
        }

        for (var tsvFile : tsvFiles) {
            hashToPath.putAll(tsvToMap(tsvFile));
        }
        return hashToPath;
    }

    private Map<String, Path> initDgkmHashToFilepath(Collection<Image> images) {
        return images.stream()
                .collect(Collectors.toMap(Image::getDgkmHash, Image::getFilePath));
    }

    private Map<String, Path> initDgkmHashToThumb(Path thumbsDirectory) {
        Map<String, Path> dgkmHashToThumbPath = new HashMap<>();
        Pattern pattern = Pattern.compile("([\\d\\w]{32})\\.(png|jpg)");

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(thumbsDirectory, "*.{png,jpg}")) {
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
        return dgkmHashToThumbPath;
    }

    private Map<String, Image> createMgckHashToImageMap() {
        return getImageObjects()
                .filter(img -> img.getImHash() != null)
                .collect(Collectors.toMap(Image::getImHash, img -> img));
    }

    private Image getImageForMgckHash(String mgckHash) {
        return mgckHashToImageMap.get(mgckHash);
    }

    private Map<String, Image> createDgkmHashToImageMap() {
        return getImageObjects()
                .filter(img -> img.getDgkmHash() != null)
                .collect(Collectors.toMap(Image::getDgkmHash, img -> img));
    }

    private Image getImageForDgkmHash(String dgkmHash) {
        return dgkmHashToImageMap.get(dgkmHash);
    }

    public Image getImageForHash(String hash) {
        if (hash.length() == 64) {
            return getImageForMgckHash(hash);
        } else if (hash.length() == 32) {
            return getImageForDgkmHash(hash);
        } else {
            log.warn("Failed to find image for hash={}", hash);
            return null;
        }
    }

    public Map<Integer, Long> getNumberOfPhotosByYear() {
        return imageObjects.stream()
                .map(Image::getCreationDate)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(LocalDateTime::getYear, Collectors.counting()));
    }

    public Stream<Image> getImageObjects() {
        return imageObjects.stream();
    }

    public Stream<Image> getImagesForTag(String tag) {
        log.info("Asked for {} tag.", tag);
        return imageObjects.stream()
                .filter(img -> img.getTags().contains(tag));
    }

    public Set<String> getTags() {
        if (tags == null) {
            tags = imageObjects.stream()
                    .flatMap(img -> img.getTags().stream())
                    .distinct()
                    .collect(Collectors.toSet());
            log.info("Found {} unique tags from {} imageObjects.", tags.size(), imageObjects.size());
        }
        return tags;
    }

    public String getTagNameExtended(String tagId) {
        return tagIdToNameMap.get(tagId);
    }

    private Map<String, String> readTagIdToNameMap(Path tagIdToNameFile) {
        var tagIdToNameMap = new HashMap<String, String>();

        try {
            Files.lines(tagIdToNameFile, Charset.forName("UTF-8"))
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

    private Map<String, Path> tsvToMap(Path tsvFile) {

        var hashToPath = new HashMap<String, Path>();

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

    public Map<String, Path> getImHashToPathMap() {
        return imHashToPath;
    }

    public Map<String, Path> getDgkmHashToThumbPathMap() {
        return dgkmHashToThumbPath;
    }

    private void fillThumbPathFields(Collection<Image> images, Map<String, Path> dgkmHashToThumbPath) {
        int thumbExists = 0, thumbNotExists = 0;
        // var stats = new LinkedHashMap<String,Integer>(Map.of("thumbExists", 0,
        // "thumbNotExists", 0));
        for (var image : images) {
            Path thumbPath = dgkmHashToThumbPath.get(image.getDgkmHash());
            image.setThumbPath(thumbPath);
            if (thumbPath == null) {
                thumbNotExists++;
            } else {
                thumbExists++;
            }
        }
        log.debug("Thumbnail paths provided/not_provided: {}/{}", thumbExists, thumbNotExists);
    }

}
