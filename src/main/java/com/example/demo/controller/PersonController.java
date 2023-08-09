package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    
    @GetMapping("/names")
    List<String> getPersonsNames() {
        var people = personRepository.findAll();
        List<String> names = people.stream().map(Person::getName).collect(Collectors.toList());
        return names;
    }

    @GetMapping("/{birthday}")
    Optional<Person> findOneByBirthday(@PathVariable("birthday") LocalDate birthday) {
        log.trace("birthday to find: {}",birthday);
        // var ld = LocalDate.parse(birthday);
        // log.trace("LocalDate parsed: {}", ld);
        // var person = personRepository.findOneByBirthday(ld);
        var person = personRepository.findOneByBirthday(birthday);
        
        return person;
    }
    
}