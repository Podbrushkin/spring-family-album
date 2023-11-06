package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.example.demo.data.ImageRepository;
import com.example.demo.data.ImageRepositoryNeo4j;
import com.example.demo.data.PersonRepository;
import com.example.demo.graphviz.GraphvizProcessor;
import com.example.demo.model.Image;
import com.example.demo.service.ImageService;


@Component
@DependsOn("graphDatabaseService")
public class Catalog {
    Logger log = LoggerFactory.getLogger(getClass());
    private Set<Image> imageObjects;
    // private Set<String> tags;
    // private ImageRepositoryNeo4j imageRepositoryNeo4j;

    public Catalog(
            ImageRepository imageRepository,
            ImageRepositoryNeo4j imageRepositoryNeo4j,
            PersonRepository personRepository,
            ImageService imageService,
            GraphvizProcessor graphvizProc,
            @Value("${filepaths.tagIdToNameFile:#{null}}") String tagIdToNameFileStr,
            @Value("${filepaths.graphvizTree:#{null}}") Path graphvizTree,
            @Value("${filepaths.imageMagickHashFiles:#{null}}") String[] imHashFiles,
            @Value("${filepaths.thumbsDirectory:#{null}}") String thumbsDirectory,
            @Value("${filepaths.whiteListDirectories:#{null}}") String[] whiteListDirectories,
            @Value("${filepaths.blackListDirectories:#{null}}") String[] blackListDirectories) {
        imageObjects = imageRepository.getImages();

        // this.imageRepositoryNeo4j = imageRepositoryNeo4j;
        
        var imagesInitialCount = imageRepositoryNeo4j.count();
        if (imagesInitialCount > 0) {
            log.trace("Database already had {} images, removing them...", imagesInitialCount);
            imageRepositoryNeo4j.deleteAll();
        }
        
        // tags = getTags();

        var mgckHashToPath = initMgckHashToFilepath(imHashFiles);
        addImHashToImgs(mgckHashToPath, imageObjects);
        mgckHashToPath = null;

        if (thumbsDirectory != null) {
            Map<String, Path> dgkmHashToThumbPath = createDgkmHashToThumbMap(Path.of(thumbsDirectory));
            fillThumbPathFields(imageObjects, dgkmHashToThumbPath);
        }
        
        whiteListDirectories = whiteListDirectories == null ? new String[0] : whiteListDirectories;
        blackListDirectories = blackListDirectories == null ? new String[0] : blackListDirectories;
        var whiteListDirs = Stream.of(whiteListDirectories).map(s -> Path.of(s)).collect(Collectors.toSet());
        var blackListDirs = Stream.of(blackListDirectories).map(s -> Path.of(s)).collect(Collectors.toSet());
        imageObjects = applyFilters(imageObjects, whiteListDirs, blackListDirs);
        
        log.trace("Persisting Image objects...");
        var imgObjs = imageRepositoryNeo4j.saveAll(imageObjects);
        log.trace("imgObjs.size() {}",imgObjs.size());

        if (graphvizTree != null) {
            // var graphvizTreePath = Path.of(graphvizTree);
            var graphvizTreePath = graphvizTree;
            if (Files.exists(graphvizTreePath)) {
                removeAllPersonsFromDbAndAddNewFromGraphviz(personRepository, graphvizProc, graphvizTreePath);
            }
        }

        createImageDepictsPersonRels(imageRepositoryNeo4j, personRepository);
    }

    private void removeAllPersonsFromDbAndAddNewFromGraphviz(PersonRepository personRepositoryNeo4j, GraphvizProcessor gp, Path dot) {
        personRepositoryNeo4j.deleteAll();
        var peop = gp.getPeople(dot);
        log.info("Graphviz file provided {} people.",peop.size());
        var s = peop.stream()
            .sorted(
                Comparator.comparing(p -> p.getFullName(), 
                    Comparator.nullsFirst(Comparator.naturalOrder()))
                )
        .map(p -> p.getFullName()).collect(Collectors.joining("\n"));
        log.info(s);
        var before = personRepositoryNeo4j.count();
        log.trace("Going to persist all created Person objects...");
        personRepositoryNeo4j.saveAll(peop);
        /* for (var p : peop) {
            log.trace("Saving {}...",p);
            personRepositoryNeo4j.save(p);
        } */
        /* var x = new Person("xxx","pupkin");
        var y = new Person("yyy","lupkin");
        x.getChildren().add(y);
        personRepositoryNeo4j.saveAll(List.of(x,y)); */

        var after = personRepositoryNeo4j.count();
        log.info("Amount of people in database changed from {} to {}.",before, after);
    }

    private Integer createImageDepictsPersonRels(ImageRepositoryNeo4j imageRepositoryNeo4j, PersonRepository personRepository) {
        int relsBefore = imageRepositoryNeo4j.countImageDepictsPersonRels();
        log.trace("There are {} Image-Depicts->Person rels.", relsBefore);

        Set<String> tagsAll = imageRepositoryNeo4j.findAll()
            .stream()
            .flatMap(img -> img.getTags().stream())
            .collect(Collectors.toSet());
        log.trace("Here are all {} tags initially provided to this app: \n{}",tagsAll.size(),tagsAll);

        log.trace("Assuring for each bday tag there is a person...");
        var bdayTags =
        tagsAll.stream()
            .filter(t -> t.matches("\\d{4}-\\d{2}-\\d{2}"))
            .map(s -> LocalDate.parse(s))
            .toList();
        var persons = personRepository.findAllByBirthdayIn(bdayTags);
        if (bdayTags.size() == persons.size()) {
            log.trace("For every birthday tag a person was found.");
        } else {
            log.warn("Birthday tags ({}) and persons ({}) aren't 1:1!", 
                    bdayTags.size(),persons.size());
            
            var bdaysFromPersons =
                persons.stream()
                .map(p -> p.getBirthday())
                .collect(Collectors.toSet());

            var bdaysWithoutPerson = new HashSet<>(bdayTags)
                .removeAll(bdaysFromPersons);
            log.warn("Here are birthdays from Image.tags which doesn't have corresponding person: {}",bdaysWithoutPerson);
        }

        log.trace("Assuring for each name tag there is a person...");
        var nameTags = tagsAll.stream()
                .filter(t -> !t.matches("\\d{4}-\\d{2}-\\d{2}"))
                .collect(Collectors.toSet());
        persons = personRepository.findAllByFullNameIn(nameTags);
        if (nameTags.size() == persons.size()) {
            log.trace("For every name tag a person was found.");
        } else {
            log.warn("Name tags ({}) and persons ({}) aren't 1:1.", 
                    nameTags.size(),persons.size());
            
            var namesFromPersons =
                persons.stream()
                .map(p -> p.getFullName())
                .collect(Collectors.toSet());

            var namesWithoutPerson = nameTags.stream()
                .filter(t -> !namesFromPersons.contains(t))
                .collect(Collectors.toSet());
            log.warn("Here are names from Image.tags which doesn't have corresponding person: {}",
                namesWithoutPerson);
            log.warn("These people wouldn't be available in app. "+
                "Consider using person's fullname in image tag if bday isn't known.");
        }


        log.trace("Creating Image-Depicts->Person rels...");
        int relsAfter = imageRepositoryNeo4j.createImageDepictsPersonRels();
        log.trace("Now there are {} Image-Depicts->Person rels, {} created.",
            relsAfter, relsAfter-relsBefore);
        return relsAfter;
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

    private void logExtensionCounts(Collection<Image> images) {
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

    private Map<String, Path> initMgckHashToFilepath(String[] imHashFiles) {
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

    private void addImHashToImgs(Map<String, Path> hashToPath, Set<Image> imgs) {
        var pathToImg =
            imgs.stream()
                .collect(Collectors.toMap(Image::getFilePath, img->img));
        

        int count = 0;
        for (var entry : hashToPath.entrySet()) {
            var img = pathToImg.get(entry.getValue());
            if (img != null) {
                img.setImHash(entry.getKey());
                count++;
            }
        }
        log.trace("Added {} imageMagick hashes to Image objs.", count);
    }

    private Map<String, Path> createDgkmHashToThumbMap(Path thumbsDirectory) {
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

    public Map<Integer, Long> getNumberOfPhotosByYear() {
        return imageObjects.stream()
                .map(Image::getCreationDate)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(LocalDateTime::getYear, Collectors.counting()));
    }

    public Stream<Image> getImageObjects() {
        return imageObjects.stream();
    }

    // private Set<String> getTags() {
    //     if (tags == null) {
    //         tags = imageObjects.stream()
    //                 .flatMap(img -> img.getTags().stream())
    //                 .distinct()
    //                 .collect(Collectors.toSet());
    //         log.info("Found {} unique tags from {} imageObjects.", tags.size(), imageObjects.size());
    //     }
    //     return tags;
    // }

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

    private void fillThumbPathFields(Collection<Image> images, Map<String, Path> dgkmHashToThumbPath) {
        int thumbExists = 0, thumbNotExists = 0;
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
