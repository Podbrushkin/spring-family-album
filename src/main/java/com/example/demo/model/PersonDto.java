package com.example.demo.model;

public class PersonDto {
    
    private String id;
    private String fullName;
    private String birthday;
    private Integer imagesCount;

    private PersonDto(String id, String name, String birthday, Integer imagesCount) {
        this.id = id;
        this.fullName = name;
        this.birthday = birthday;
        this.imagesCount = imagesCount;
    }
    public PersonDto(Person person) {
        this.id = person.getDotId();
        this.fullName = person.getFullName();
        if (person.getBirthday() != null)
            this.birthday = person.getBirthday().toString();
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
    public void setFullName(String name) {
        this.fullName = name;
    }
    public String getBirthday() {
        return birthday;
    }
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
    public String getBirthdayOrFullName() {
        return birthday == null ? this.fullName : this.birthday.toString();
    }
    public Integer getImagesCount() {
        return imagesCount;
    }
    public void setImagesCount(Integer imagesCount) {
        this.imagesCount = imagesCount;
    }
    
}
