package com.example.demo.data;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Image;

@Repository
// @DependsOn("imageResultSetExtractor")
@Primary
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

    public int getImagesCount() {
        return images.size();
        // String sql = "SELECT COUNT(*) FROM Images";
        // return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /*
     * private static class ImageRowMapper implements RowMapper<Image> {
     * 
     * @Value("${filepaths.dgkmRoot}")
     * private String dgkmRoot;
     * 
     * @Override
     * public Image mapRow(ResultSet rs, int rowNum) throws SQLException {
     * Image image = new Image();
     * image.setDgkmHash(rs.getString("uniqueHash"));
     * image.setCreationDate(rs.getTimestamp("creationDate").toLocalDateTime());
     * String relativePath = rs.getString("relativePath");
     * String imgName = rs.getString("imgName");
     * Path filePath = Paths.get(dgkmRoot, relativePath, imgName);
     * image.setFilePath(filePath);
     * Set<String> tags = new HashSet<>();
     * tags.add(rs.getString("tagName"));
     * image.setTags(tags);
     * return image;
     * }
     * }
     */
}