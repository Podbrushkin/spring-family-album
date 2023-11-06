package com.example.demo.data;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Image;
import com.example.demo.databasePopulating.ImageResultSetExtractor;

@Repository
// @Primary
@ConditionalOnProperty({"jdbc.driverClassName","jdbc.url","filepaths.dgkmRoot"})
public class ImageRepositoryJdbc implements ImageRepository {
    Logger log = LoggerFactory.getLogger(getClass());
    private final JdbcTemplate jdbcTemplate;
    ImageResultSetExtractor imageResultSetExtractor;
    private Set<Image> images;

    public ImageRepositoryJdbc(JdbcTemplate jdbcTemplate, ImageResultSetExtractor imageResultSetExtractor) {
        this.jdbcTemplate = jdbcTemplate;
        this.imageResultSetExtractor = imageResultSetExtractor;
        this.images = readImages();
        
    }

    private Set<Image> readImages() {
        String sql = """
                SELECT uniqueHash, alb.relativePath, img.name AS imgName, creationDate, Tags.name AS tagName
                FROM Images img
                INNER JOIN Albums alb ON img.album = alb.id
                INNER JOIN ImageTags imgTags ON imgTags.imageid = img.id
                INNER JOIN Tags ON imgTags.tagid = Tags.id
                INNER JOIN ImageInformation imgInf ON imgInf.imageid = img.id
                WHERE Tags.pid = 4
                """;
        Map<String, Image> imageMap = jdbcTemplate.query(sql, imageResultSetExtractor);
        log.info("{} images have been read from sqlite db.", imageMap.size());

        try {
            jdbcTemplate.getDataSource().getConnection().close();
        } catch (SQLException e) {
            log.error("Failed to close database connection:", e);
        }

        return imageMap.values().stream().collect(Collectors.toSet());
        // return jdbcTemplate.query(sql, new ImageRowMapper());
    }

    @Override
    public Set<Image> getImages() {
        return images;
    }
}