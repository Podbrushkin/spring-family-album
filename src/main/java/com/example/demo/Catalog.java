package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.demo.data.ImageRepository;
import com.example.demo.model.Image;
import com.example.demo.model.Tag;

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
    private Map<String, Path> dgkmHashToThumbPath = new HashMap<>();

    public Catalog(
            ImageRepository imageRepository,
            @Value("${filepaths.tagIdToNameFile:#{null}}") String tagIdToNameFileStr,
            @Value("${filepaths.imageMagickHashFiles:#{null}}") String[] imHashFiles,
            @Value("${filepaths.thumbsDirectory:#{null}}") String thumbsDirectory,
            @Value("${filepaths.whiteListDirectories:#{null}}") String[] whiteListDirectories,
            @Value("${filepaths.blackListDirectories:#{null}}") String[] blackListDirectories) {
        imageObjects = imageRepository.getImages();
        // if (tagIdToNameFileStr != null) {
        this.tagIdToNameFile = tagIdToNameFileStr == null ? null : Path.of(tagIdToNameFileStr);
        this.tagIdToNameMap = readTagIdToNameMap(tagIdToNameFile);
        
        
        // imageObjects = readImageObjectsFromJson(imageObjectsJson);
        mgckHashToImageMap = createMgckHashToImageMap();
        dgkmHashToImageMap = createDgkmHashToImageMap();
        tags = getTags();

        this.imHashToPath = initHashToFilepath(imHashFiles);
        this.imHashToPath.putAll(initDgkmHashToFilepath(imageObjects));
        log.trace("imHashToPath.size(): {}",imHashToPath.size());
        if (thumbsDirectory != null) {
            this.dgkmHashToThumbPath = initDgkmHashToThumb(Path.of(thumbsDirectory));
            fillThumbPathFields(imageObjects, dgkmHashToThumbPath);
        }
        
        whiteListDirectories = whiteListDirectories == null ? new String[0] : whiteListDirectories;
        blackListDirectories = blackListDirectories == null ? new String[0] : blackListDirectories;
        var whiteListDirs = Stream.of(whiteListDirectories).map(s -> Path.of(s)).collect(Collectors.toSet());
        var blackListDirs = Stream.of(blackListDirectories).map(s -> Path.of(s)).collect(Collectors.toSet());
        imageObjects = applyFilters(imageObjects, whiteListDirs, blackListDirs);
    }

    private Set<Image> applyFilters(Collection<Image> imageObjects, Set<Path> whiteListDirs, Set<Path> blackListDirs) {
        final var whiteListDirsSafe = whiteListDirs == null ? new HashSet<Path>() : whiteListDirs;
        final var blackListDirsSafe = blackListDirs == null ? new HashSet<Path>() : blackListDirs;

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
                    boolean allowedByWhiteListedDirs = whiteListDirsSafe.isEmpty()
                            ? true
                            : whiteListDirsSafe.stream()
                                    .anyMatch(p -> imgPath.startsWith(p));
                    boolean allowedByBlackListedDirs = blackListDirsSafe.stream()
                            .allMatch(p -> !imgPath.startsWith(p));
                    return exists && allowedByWhiteListedDirs && allowedByBlackListedDirs;
                })
                .collect(Collectors.toSet());
        int after = result.size();
        log.debug("Images dismissed by extension: {}", arr[0]);
        
        String msg = "By whiteListDirs({}) and blackListDirs({}) and extension amount of images changed from {} to {}, by {}.";
        log.debug(msg, whiteListDirsSafe.size(), blackListDirsSafe.size(), before, after, after - before);
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
        var numOfFiles = imHashFiles == null ? 0 : imHashFiles.length;
        log.trace("Найдено {} paths to files with imageMagick hashes.", numOfFiles);
        Map<String, Path> hashToPath = new HashMap<>();
        if (numOfFiles == 0) {return hashToPath;}

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

    public List<Image> getImagesForTags(Collection<String> tags, Optional<Integer> yearOpt) {

        List<Image> imageList = this.getImageObjects()
                .filter(img -> img.getTags().containsAll(tags))
                .filter(img -> {
                    return (yearOpt.isPresent()) ? ((Integer) img.getCreationDate().getYear()).equals(yearOpt.get())
                            : true;
                })
                .toList();

        int totalItems = imageList.size();
        // imageList.sort(Comparator.comparing(Image::getCreationDate));
        return imageList;
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


    /*
     * A collection of tagged images implies there is a set of tags which you can choose,
     * this method returns this tags with additional info.
     */
    private List<Tag> selectableTags = null;
    public List<Tag> getSelectableTagObjs() {
        if (selectableTags == null) {
            log.trace("Calculating selectable tags...");
            List<Tag> tags = new ArrayList<Tag>();
            this.getTags().forEach(s -> {
                var imageList = this.getImagesForTags(List.of(s), Optional.empty());
                long imagesCount = imageList.size();
                tags.add(new Tag(s, this.getTagNameExtended(s), imagesCount));
            });
            tags.sort(Comparator.comparing(t -> t.getName()));
            this.selectableTags = tags;
            log.trace("{} selectableTags have been found.", tags.size());
        }
		return selectableTags;
	}
    public List<Tag> getTagObjs(List<String> tagIds) {
        /* tagId -> {
            this.getSelectableTagObjs().stream().filter(t -> t.getName().equals(tagId));
        } 
        tagId -> {
            return
                this.getSelectableTagObjs().stream().filter(t -> t.getName().equals(tagId)).findFirst();
        }
        */
        return 
        tagIds.stream()
        .map(tagId -> {
            return
                this.getSelectableTagObjs()
                .stream()
                .filter(t -> t.getId().equals(tagId))
                .findFirst()
                .orElse(null);
        })
        .toList();
    }

    public String getTagNameExtended(String tagId) {
        return tagIdToNameMap.get(tagId);
    }

    private Map<String, String> readTagIdToNameMap(Path tagIdToNameFile) {
        var tagIdToNameMap = new HashMap<String, String>();
        if (tagIdToNameFile == null) return tagIdToNameMap;
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
        log.trace("{} tag extended names have been read from tsv file.", tagIdToNameMap.size());
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
