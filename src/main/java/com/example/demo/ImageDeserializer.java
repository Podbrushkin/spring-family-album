package com.example.demo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

import com.example.demo.model.Image;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ImageDeserializer extends JsonDeserializer<Image> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_OFFSET = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_WITHOUT_OFFSET = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Override
    public Image deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = mapper.readTree(jp);

        String imageHash = node.get("imageHash").asText();
        String dgkmHash = node.get("uniqueHash").asText();
        String creationDateStr = node.get("creationDate").asText();
        LocalDateTime creationDate = parseDateWithOffset(creationDateStr);
        String fullName = node.get("fullName").asText();

        Set<String> tags = new HashSet<>();

        JsonNode tagsNode = node.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                tags.add(tagNode.asText());
            }
        }

        return new Image(creationDate, fullName, imageHash, dgkmHash, tags);
    }

    private LocalDateTime parseDateWithOffset(String dateStr) {
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateStr, DATE_TIME_FORMATTER_WITH_OFFSET);
            return offsetDateTime.toLocalDateTime();
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(dateStr, DATE_TIME_FORMATTER_WITHOUT_OFFSET);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }
}