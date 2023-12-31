package com.example.demo.data;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.example.demo.model.Image;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ImageDeserializer extends JsonDeserializer<Image> {
    DateParser dateParser;
    public ImageDeserializer(DateParser dateParser) {
        super();
        this.dateParser = dateParser;
    }
    
    @Override
    public Image deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode node = mapper.readTree(jp);

        String imageHash = node.get("imageHash").asText();
        String dgkmHash = node.get("uniqueHash").asText();
        String creationDateStr = node.get("creationDate").asText();
        LocalDateTime creationDate = dateParser.parseDateTime(creationDateStr);
        String fullName = node.get("fullName").asText();
        if (fullName.contains("classpath:")) {
            fullName = fullName.replace("classpath:", "").replace("\\", "/");
            fullName = new ClassPathResource(fullName).getFile().toPath().toString();
        }

        Set<String> tags = new HashSet<>();

        JsonNode tagsNode = node.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                tags.add(tagNode.asText());
            }
        }

        return new Image(creationDate, fullName, imageHash, dgkmHash, tags);
    }

    
}