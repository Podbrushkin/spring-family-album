package com.example.demo.data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Component;

@Component
public class DateParser {
    private static final DateTimeFormatter DATE_TIME_FORMATTER_WITH_OFFSET = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static final DateTimeFormatter DATE_TIME_FORMATTER_WITHOUT_OFFSET = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");


    public LocalDateTime parseDateTime(String dateStr) {
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
