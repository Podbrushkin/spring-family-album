package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.data.PersonRepository;
import com.example.demo.model.Person;

@RestController
@RequestMapping("/persons")
public class PersonController {
    Logger log = LoggerFactory.getLogger(getClass());

    PersonRepository personRepository;
    public PersonController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @GetMapping("/")
    List<Person> getPersons() {
        return personRepository.findAll();
    }

    
    @GetMapping("/names")
    List<String> getPersonsNames() {
        var people = personRepository.findAll();
        List<String> names = people.stream().map(Person::getFullName).collect(Collectors.toList());
        return names;
    }

    @GetMapping("/{birthday}")
    Person findOneByBirthday(@PathVariable("birthday") LocalDate birthday) {
        log.trace("birthday to find: {}",birthday);
        var person = personRepository.findOneByBirthday(birthday).orElse(null);
        
        return person;
    }
    
}