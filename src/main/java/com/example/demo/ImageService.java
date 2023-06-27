package com.example.demo;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                // .filter(img -> (year.isBlank()) ? true : (img.getCreationDate().getYear() +
                // "").equals(year))
                .filter(img -> {
                    return (yearOpt.isPresent()) ? ((Integer) img.getCreationDate().getYear()).equals(yearOpt.get())
                            : true;
                })
                .toList();

        int totalItems = imageList.size();
        imageList = imageList.stream()
                .sorted(Comparator.comparing(Image::getCreationDate))
                .skip(perPage * (page - 1))
                .limit(perPage)
                .toList();

        int totalPages = (int) Math.ceil((double) totalItems / perPage);
        return new Page<Image>(imageList, page, totalPages, totalItems);
    }

    public List<Image> getAllImagesForTags(Collection<String> tags, Optional<Integer> yearOpt) {
        List<Image> imageList = catalog.getImageObjects()
                .filter(img -> img.getTags().containsAll(tags))
                // .filter(img -> (year.isBlank()) ? true : (img.getCreationDate().getYear() +
                // "").equals(year))
                .filter(img -> {
                    return (yearOpt.isPresent()) ? ((Integer) img.getCreationDate().getYear()).equals(yearOpt.get())
                            : true;
                })
                .sorted(Comparator.comparing(Image::getCreationDate))
                .toList();
        return imageList;
    }

    public Map<String, Image> getNextAndPreviousImages(Collection<String> tags, Optional<Integer> yearOpt,
            String curImageImHash) {
        var imageList = getAllImagesForTags(tags, yearOpt);
        Map<String, Image> result = new HashMap<>();
        for (int i = 0; i < imageList.size(); i++) {
            var curHash = imageList.get(i).getImHash();
            if (curHash.equals(curImageImHash)) {
                if (i != 0) {
                    result.put("previous", imageList.get(i - 1));
                }
                if (i != imageList.size()-1) {
                    result.put("next", imageList.get(i + 1));
                }
                break;
            }
        }
        return result;
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
}
