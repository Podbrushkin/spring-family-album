package com.example.demo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Image;

@Service
public class ImageService {
    Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    Catalog catalog;

    public Page<Image> getImagesForTags(Collection<String> tags, Optional<Integer> yearOpt, int page, int perPage) {

        List<Image> imageList = catalog.getImageObjects()
                .filter(img -> img.getTags().containsAll(tags))
                // .filter(img -> (year.isBlank()) ? true : (img.getCreationDate().getYear() + "").equals(year))
                .filter(img -> {
                    return
                    (yearOpt.isPresent()) ? ((Integer)img.getCreationDate().getYear()).equals(yearOpt.get()) : true;
                })
                .toList();

        int totalItems = imageList.size();
        imageList = 
            imageList.stream()
                .sorted(Comparator.comparing(Image::getCreationDate))
                .skip(perPage * (page - 1))
                .limit(perPage)
                .toList();
        
        int totalPages = (int) Math.ceil((double) totalItems / perPage);
        return new Page<Image>(imageList, page, totalPages, totalItems);
    }

    public List<Image> getImagesForTagsOld(Collection<String> tags, String year, int page, int perPage) {

        var imagesStream = catalog.getImageObjects()
                .filter(img -> img.getTags().containsAll(tags))
                .filter(img -> (year.isBlank()) ? true : (img.getCreationDate().getYear() + "").equals(year));

        return imagesStream
                .sorted(Comparator.comparing(Image::getCreationDate))
                .skip(perPage * (page - 1))
                .limit(perPage)
                .toList();
    }

    /* public int countImagesForTags(Collection<String> tags) {
        var resultHashes = new TreeSet<String>();
        var baseSet = catalog.getImagesForTag(tags.iterator().next())
                .map(img -> img.getImageHash())
                .collect(Collectors.toSet());
        ;
        resultHashes.addAll(baseSet);
        for (String tag : tags) {
            // var newPortion = catalog.getHashesForTag(tag);
            var newPortion = catalog.getImagesForTag(tag)
                    .map(img -> img.getImageHash())
                    .collect(Collectors.toSet());
            log.info("{} has {} photos.", tag, newPortion.size());
            resultHashes.retainAll(newPortion);
        }
        log.info("{} photos found.", resultHashes.size());
        return resultHashes.size();
    } */
}
