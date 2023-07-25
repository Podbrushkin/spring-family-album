package com.example.demo.data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import com.example.demo.model.Image;

@Component
@ConditionalOnBean(ImageRepositoryJdbc.class)
class ImageResultSetExtractor implements ResultSetExtractor<Map<String, Image>> {
    Logger log = LoggerFactory.getLogger(getClass());
    @Value("${filepaths.dgkmRoot}")
    private String dgkmRoot;
    DateParser dateParser;

    ImageResultSetExtractor(DateParser dateParser) {
        super();
        this.dateParser = dateParser;
    }

    @Override
    public Map<String, Image> extractData(ResultSet rs) throws SQLException {
        Map<String, Image> imageMap = new HashMap<>();

        while (rs.next()) {
            String dgkmHash = rs.getString("uniqueHash");
            String relativePath = rs.getString("relativePath");
            String imgName = rs.getString("imgName");
            // var creationDate = rs.getTimestamp("creationDate").toLocalDateTime();
            var creationDate = dateParser.parseDateTime(rs.getString("creationDate"));
            String tagName = rs.getString("tagName");

            Path filePath = Paths.get(dgkmRoot, relativePath, imgName);

            Image image = imageMap.get(dgkmHash);
            if (image == null) {
                image = new Image();
                image.setDgkmHash(dgkmHash);
                image.setCreationDate(creationDate);
                image.setFilePath(filePath);
                image.setTags(new HashSet<>());
                imageMap.put(dgkmHash, image);
            }

            image.getTags().add(tagName);
            //log.trace("Image have been read: {}", image);
        }

        return imageMap;
    }
}