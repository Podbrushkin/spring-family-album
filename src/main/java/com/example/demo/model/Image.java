package com.example.demo.model;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

@Node("Image")
public class Image {

    @Id
    @GeneratedValue
    String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String imHash;
    private String dgkmHash;
    private LocalDateTime creationDate;
    private String filePath;
    private String thumbPath;
    private Set<String> tags;

    @Relationship(type = "DEPICTS", direction = Direction.OUTGOING)
    private Set<Person> people  = new HashSet<>();

    public Set<Person> getPeople() {
        return people;
    }

    public void setPeople(Set<Person> people) {
        this.people = people;
    }

    public Image() {
    }

    public Image(
            LocalDateTime creationDate,
            String fullName,
            String imHash) {

        this.creationDate = creationDate;
        this.filePath = fullName;
        this.imHash = imHash;
    }

    public Image(
            LocalDateTime creationDate,
            String fullName,
            String imHash,
            Set<String> tags) {

        this.creationDate = creationDate;
        this.filePath = fullName;
        this.imHash = imHash;
        this.tags = tags;
    }

    public Image(
            LocalDateTime creationDate,
            String fullName,
            String imHash,
            String dgkmHash,
            Set<String> tags) {

        this.creationDate = creationDate;
        this.filePath = fullName;
        this.imHash = imHash;
        this.dgkmHash = dgkmHash;
        this.tags = tags;
    }

    public Image(String hash, Set<String> tags) {
        this.imHash = hash;
        this.tags = tags;
    }

    public Image(String hash) {
        this.imHash = hash;
    }

    public String getDgkmHash() {
        return dgkmHash;
    }

    public void setDgkmHash(String dgkmHash) {
        this.dgkmHash = dgkmHash;
    }

    public Path getFilePath() {
        return Path.of(filePath);
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath.toString();
    }

    public void setImHash(String hash) {
        this.imHash = hash;
    }

    public void setCreationDate(LocalDateTime dateTaken) {
        this.creationDate = dateTaken;
    }

    public String getImHash() {
        return imHash;
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
        return (thumbPath == null) ? null : Path.of(thumbPath);
    }

    public void setThumbPath(Path thumbPath) {
        if (thumbPath != null)
            this.thumbPath = thumbPath.toString();
    }

    public String getAnyHash() {
        if (imHash != null)
            return imHash;
        else if (dgkmHash != null)
            return dgkmHash;
        else
            return null;
    }

    @Override
    public String toString() {
        return "Image [imHash=" + imHash + ", dgkmHash=" + dgkmHash + ", creationDate=" + creationDate + ", filePath="
                + filePath + ", thumbPath=" + thumbPath + ", tags=" + tags + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((imHash == null) ? 0 : imHash.hashCode());
        result = prime * result + ((dgkmHash == null) ? 0 : dgkmHash.hashCode());
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
        if (imHash == null) {
            if (other.imHash != null)
                return false;
        } else if (!imHash.equals(other.imHash))
            return false;
        if (dgkmHash == null) {
            if (other.dgkmHash != null)
                return false;
        } else if (!dgkmHash.equals(other.dgkmHash))
            return false;
        return true;
    }

}
