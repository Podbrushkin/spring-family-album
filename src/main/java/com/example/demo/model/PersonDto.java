package com.example.demo.model;

public class PersonDto {
    
    private String id;
    private String name;
    private String birthday;
    private Integer imagesCount;

    public PersonDto(String id, String name, String birthday, Integer imagesCount) {
        this.id = id;
        this.name = name;
        this.birthday = birthday;
        this.imagesCount = imagesCount;
    }
    public PersonDto(Person person) {
        this.id = person.getId();
        this.name = person.getName();
        if (person.getBirthday() != null)
            this.birthday = person.getBirthday().toString();
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getBirthday() {
        return birthday;
    }
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
    public Integer getImagesCount() {
        return imagesCount;
    }
    public void setImagesCount(Integer imagesCount) {
        this.imagesCount = imagesCount;
    }
    
}
