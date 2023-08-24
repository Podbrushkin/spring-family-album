package com.example.demo.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.PersonService;

@RestController
@RequestMapping("/graph")
public class GraphController {
    Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    PersonService personService;

    @GetMapping
    public Map<String, List<Object>> getGraph() {
		return personService.fetchPersonGraph();
	}
    @GetMapping("/{personId}")
    public Map<String, List<Object>> getPersonAncestorsGraph(
        @PathVariable(required = true) String personId
        ) {
        
		var p = personService.findOneById(personId);
        if (p.isPresent()) {
            return personService.fetchPersonAncestorsGraph(p.get());
        } else {
            log.warn("Person with id={} wasn't found.",personId);
            return null;
        }
            
        
	}
}
