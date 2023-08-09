package com.example.demo.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.data.ImageRepositoryNeo4j;

import com.example.demo.model.Image;
import com.example.demo.model.Page;

@Service
public class ImageService {
    Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    PersonService personService;
    @Autowired
    ImageRepositoryNeo4j imageRepositoryNeo4j;
    Comparator<Image> imgComparator = Comparator.comparing(Image::getCreationDate);

    public Image getImageForHash(String hash) {
        return imageRepositoryNeo4j.findOneByImHashOrDgkmHash(hash, hash);
    }

    public Page<Image> getImagesForTagsPageable(Collection<String> tags, Optional<Integer> yearOpt, int page, int perPage) {

        List<Image> imageList = this.getAllImagesForTags(tags, yearOpt);

        int totalItems = imageList.size();
        imageList = imageList.stream()
                .skip(perPage * (page - 1))
                .limit(perPage)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalItems / perPage);
        return new Page<Image>(imageList, page, totalPages, totalItems);
    }

    public List<Image> getAllImagesForTags(Collection<String> tags, Optional<Integer> yearOpt) {
        var imageList = 
            imageRepositoryNeo4j.findByTagsContainsAll(tags);
        Collections.sort(imageList, imgComparator);
        if (yearOpt.isPresent()) {
            imageList =
            imageList.stream()
            .filter(img -> {
                if (yearOpt.isPresent()) {
                    return 
                        ((Integer) img.getCreationDate().getYear()).equals(yearOpt.get());
                } else 
                    return true;
            })
            .collect(Collectors.toList());
        }
        return imageList;
    }

    public Map<String, Image> getNextAndPreviousImages(Collection<String> tags, Optional<Integer> yearOpt,
            Image img) {
        var imageList = getAllImagesForTags(tags, yearOpt);
        String providedImgHash = img.getAnyHash();
        Map<String, Image> result = new HashMap<>();
        for (int i = 0; i < imageList.size(); i++) {
            String curHash = imageList.get(i).getAnyHash();

            if (curHash.equals(providedImgHash)) {
                if (i != 0) {
                    result.put("previous", imageList.get(i - 1));
                }
                if (i != imageList.size()-1) {
                    result.put("next", imageList.get(i + 1));
                }
                break;
            }
        }
        log.trace("Found prev and next images for {}: {}",img.getFilePath(), result);
        return result;
    }
}
