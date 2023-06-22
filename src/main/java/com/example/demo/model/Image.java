package com.example.demo.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

public class Image {

    private String imageHash;
    private String dgkmHash;
    private LocalDateTime creationDate;
    private Path filePath;
    private Path thumbPath;
    private Set<String> tags;

    public Image(
            LocalDateTime creationDate,
            String fullName,
            String imageHash) {

        this.creationDate = creationDate;
        this.filePath = Path.of(fullName);
        this.imageHash = imageHash;
    }

    public Image(
            LocalDateTime creationDate,
            String fullName,
            String imageHash,
            Set<String> tags) {

        this.creationDate = creationDate;
        this.filePath = Path.of(fullName);
        this.imageHash = imageHash;
        this.tags = tags;
    }

    public Image(
            LocalDateTime creationDate,
            String fullName,
            String imageHash,
            String dgkmHash,
            Set<String> tags) {

        this.creationDate = creationDate;
        this.filePath = Path.of(fullName);
        this.imageHash = imageHash;
        this.dgkmHash = dgkmHash;
        this.tags = tags;
    }

    public Image(String hash, Set<String> tags) {
        this.imageHash = hash;
        this.tags = tags;
    }

    public Image(String hash) {
        this.imageHash = hash;
    }

    public String getDgkmHash() {
        return dgkmHash;
    }

    public void setDgkmHash(String dgkmHash) {
        this.dgkmHash = dgkmHash;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }

    public void setImageHash(String hash) {
        this.imageHash = hash;
    }

    public void setCreationDate(LocalDateTime dateTaken) {
        this.creationDate = dateTaken;
    }

    public String getImageHash() {
        return imageHash;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
    public Path getThumbPath() {
        return thumbPath;
    }

    public void setThumbPath(Path thumbPath) {
        this.thumbPath = thumbPath;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((imageHash == null) ? 0 : imageHash.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Image other = (Image) obj;
        if (imageHash == null) {
            if (other.imageHash != null)
                return false;
        } else if (!imageHash.equals(other.imageHash))
            return false;
        return true;
    }

}
