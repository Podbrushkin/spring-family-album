package com.example.demo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.data.ImageRepositoryNeo4j;
import com.example.demo.data.PersonRepository;
import com.example.demo.model.Person;
import com.example.demo.model.PersonDto;

@Service
public class PersonService {
    Logger log = LoggerFactory.getLogger(getClass());
    PersonRepository personRepository;
    ImageRepositoryNeo4j imageRepositoryNeo4j;
    Collection<PersonDto> allDepictedWithCountsDto;

    public PersonService(PersonRepository personRepository, ImageRepositoryNeo4j imageRepositoryNeo4j) {
        this.imageRepositoryNeo4j = imageRepositoryNeo4j;
        this.personRepository = personRepository;
    }

    public Collection<Person> getPersonsForTags(Collection<String> tags) {
        List<Person> persons = new ArrayList<>();
        var bdayTags = tags.stream()
                .filter(t -> t.matches("\\d{4}-\\d{2}-\\d{2}"))
                .map(s -> LocalDate.parse(s))
                .toList();

        persons.addAll(personRepository.findAllByBirthdayIn(bdayTags));
        log.trace("Found {} persons by bday", persons.size());

        var nameTags = tags.stream()
                .filter(t -> !t.matches("\\d{4}-\\d{2}-\\d{2}"))
                .toList();

        persons.addAll(personRepository.findAllByNameIn(nameTags));
        log.trace("Found {} persons for tags: {}.", persons.size(), tags);
        return persons;
    }

    public Collection<Person> findAllDepictedByAtLeastOneImage() {
        return personRepository.findAllDepictedByAtLeastOneImage();
    }

    /* public Collection<Person> findAllDepictedByAtLeastOneImageDto() {
        var persons = findAllDepictedByAtLeastOneImage();
        for (var p : persons) {

        }
    } */
    public Collection<PersonDto> findAllDepictedWithCountsDto() {
        if (this.allDepictedWithCountsDto != null) 
            return allDepictedWithCountsDto;

        // Map<Person,Integer> personsMap = 
        var persons = personRepository.findAllDepictedByAtLeastOneImage();
        // log.trace("got this: {}",delme);
        // return null;
        var dtos = new ArrayList<PersonDto>();
        for (var person : persons) {
            var dto = new PersonDto(person);
            // var imagesCount = personRepository.countImagesForPersonId(person.getId());
            var imagesCount = imageRepositoryNeo4j.countByPeopleContains(person.getId());
            log.trace("{} is depicted in {} images.",person, imagesCount);
            dto.setImagesCount(imagesCount);
            dtos.add(dto);
        }
        this.allDepictedWithCountsDto = dtos;
        return dtos;
        // var persons = findAllDepictedByAtLeastOneImage();
        // Map<Person,Integer> map = countImagesForPersons(persons);
    }
    
}
