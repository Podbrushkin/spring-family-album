package com.example.demo.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Node("Person")
public class Person {
    
    @Id
    @GeneratedValue
    String id;
    
    private String dotId;
    

    private String fullName;
    private LocalDate birthday;

    @Relationship(type = "HAS_CHILD", direction = Direction.OUTGOING)
    @JsonIgnoreProperties
	private Set<Person> children = new HashSet<>();
    
    @Relationship(type = "MARRIED_TO", direction = Direction.OUTGOING)
    @JsonIgnoreProperties
	private Person spouse;

    public Person(String dotId, String fullName) {
        this.dotId = dotId;
        this.fullName = fullName;
    }
    public String getDotId() {
        return dotId;
    }

    public void setDotId(String dotId) {
        this.dotId = dotId;
    }

    public LocalDate getBirthday() {
        return birthday;
    }
    public String getBirthdayOrFullName() {
        return birthday == null ? this.fullName : this.birthday.toString();
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public Set<Person> getChildren() {
        return children;
    }

    public void setChildren(Set<Person> children) {
        this.children = children;
    }

    public Person getSpouse() {
        return spouse;
    }

    public void setSpouse(Person spouse) {
        this.spouse = spouse;
    }

   

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullname) {
        this.fullName = fullname;
    }
    public String getFirstName() {
        String[] tokens = getFullName().split(" ");
        String name =
        Arrays.stream(tokens)
            .filter(s -> !s.contains("("))
            .skip(1)
            .limit(1)
            .findAny()
            .orElse(getFullName());
        return name;
    }

    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((dotId == null) ? 0 : dotId.hashCode());
        result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
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
        Person other = (Person) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (dotId == null) {
            if (other.dotId != null)
                return false;
        } else if (!dotId.equals(other.dotId))
            return false;
        if (fullName == null) {
            if (other.fullName != null)
                return false;
        } else if (!fullName.equals(other.fullName))
            return false;
        return true;
    }
    @Override
    public String toString() {
        // String childrenStr = "";
        // getChildren().size()+"";
        String childrenStr = 
            getChildren()
            .stream()
            .map(p -> p.getFullName())
            .collect(Collectors.joining(", "));
        String spouseStr = spouse == null ? null : spouse.getFullName();
        return "Person [id=" + id + ", dotId=" + dotId + ", fullName=" + fullName + ", birthday=" + birthday
                + ", children=" + childrenStr + ", spouse=" + spouseStr + "]";
    }
    
    
    
}
