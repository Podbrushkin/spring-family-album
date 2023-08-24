package com.example.demo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.data.ImageRepositoryNeo4j;
import com.example.demo.data.PersonRepository;
import com.example.demo.graphviz.GraphvizProcessor;
import com.example.demo.model.Person;
import com.example.demo.model.PersonDto;

import org.neo4j.driver.Value;

@Service
public class PersonService {
    Logger log = LoggerFactory.getLogger(getClass());
    PersonRepository personRepository;
    ImageRepositoryNeo4j imageRepositoryNeo4j;
    Collection<PersonDto> allDepictedWithCountsDto;
    @Autowired
    GraphvizProcessor graphvizProcessor;

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

        persons.addAll(personRepository.findAllByFullNameIn(nameTags));
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
            var imagesCount = imageRepositoryNeo4j.countByPeopleContains(person.getFullName());
            log.trace("{} is depicted in {} images.",person, imagesCount);
            dto.setImagesCount(imagesCount);
            dtos.add(dto);
        }
        this.allDepictedWithCountsDto = dtos;
        Collections.sort(dtos, Comparator.comparing(p -> p.getFullName()));
        return dtos;
        // var persons = findAllDepictedByAtLeastOneImage();
        // Map<Person,Integer> map = countImagesForPersons(persons);
    }

    public String getMermaidGraphFor(String personFullname) {
        var peopleFound = 
            personRepository.findAllByFullNameIn(List.of(personFullname));
        
        
        if (peopleFound.size() != 1) {
            var msg = String.format("Asked graph for fullname=%s but such person not found",personFullname);
            throw new IllegalArgumentException(msg);
        }
        return getMermaidGraphFor(peopleFound.get(0));
    }
    public String getMermaidGraphFor(Person person) {
        StringBuilder mermGraph = new StringBuilder();
        mermGraph.append("graph LR\n");
        mermGraph.append(getMermaidChildren(person));
        log.trace("Built mermaid: \n{}", mermGraph);
        /* var smpl = """
            graph LR
                A[Square Rect] -- Link text --> B((Circle))
                A --> C(Round Rect)
                B --> D{Rhombus}
                C --> D
            """;
        return smpl; */
        return mermGraph.toString();
    }

    private String getMermaidChildren(Person parent) {
        log.trace("Asked for ancestors of {}", parent.getFullName());

        var prsnName = parent.getFullName();
        StringBuilder mermGraph = new StringBuilder();
        log.trace("{} has {} children.",parent.getFullName(), parent.getChildren().size());
        for (var ch : parent.getChildren()) {
            var chName = ch.getFullName();
            // var prntHash = parent.
            var prntNode = String.format("%s[\"%s\"]",parent.hashCode(),prsnName);
            var chldNode = String.format("%s[\"%s\"]",ch.hashCode(),chName);

            var line = String.format("\t%s --> %s%n",prntNode, chldNode);
            mermGraph.append(line);
        }
        log.trace("Built these 1lvl ancestors: \n{}", mermGraph.toString());
        log.trace("Does {} have grandchildren?", parent.getFullName());
        for (var ch : parent.getChildren()) {
            var s = getMermaidChildren(ch);
            mermGraph.append(s);
        }
        log.trace("Declaration of {} and all his ancestors: \n{}", parent.getFullName(),mermGraph.toString());
        return mermGraph.toString();
    }

    @Autowired
    private Driver driver;

    public Map<String, List<Object>> fetchPersonGraph() {
        var people = this.findAll();
        return buildJsonGraphOnHasChildRel(people);
    }

    public Map<String, List<Object>> fetchPersonAncestorsGraph(Person person) {
        // var people = this.findAll();
        return buildJsonGraphOnHasChildRel(List.of(person));
    }

    public Map<String, List<Object>> buildJsonGraphOnHasChildRel(List<Person> people) {
        var nodes = new ArrayList<>();
		var links = new ArrayList<>();

        
        for (var person : people) {
            var parent = Map.of("label", "person", 
                                "title", person.getFullName(),
                                "id",person.getId());

            nodes.add(parent);
            for (var child : person.getChildren()) {
                /* var childJson = Map.of("label", "person", 
                                        "title", child.getFullName(),
                                        "id",child.getId()); */
            
                var link = Map.of("source",person.getId(),
                    "target", child.getId());
                links.add(link);
            }
        }
        return Map.of("nodes", nodes, "links", links);
    }

    public Map<String, List<Object>> fetchPersonGraphOld() {
        log.trace("Asked for person graph");
		var nodes = new ArrayList<>();
		var links = new ArrayList<>();

		try (Session session = driver.session()) {
            String query = """
				MATCH (prnt:Person) - [r:HAS_CHILD] -> (chld:Person)
				WITH prnt, chld
				RETURN prnt.fullName AS parent, collect(chld.fullName) AS children
                """;

            var records = 
                session.executeRead(tx -> tx.run(query).list());
			
			records.forEach(record -> {
				var parent = Map.of("label", "person", 
                                    "title", record.get("parent").asString());

				int sourceIndex; // = nodes.size();

                if (nodes.contains(parent)) {
				    sourceIndex = nodes.indexOf(parent);
                } else {
                    nodes.add(parent);
                    sourceIndex = nodes.size() - 1;
                }

				record.get("children").asList(Value::asString).forEach(name -> {
					var child = Map.of("label", "person", "title", name);

					int targetIndex;
					if (nodes.contains(child)) {
						targetIndex = nodes.indexOf(child);
					} else {
						nodes.add(child);
						targetIndex = nodes.size() - 1;
					}
					links.add(Map.of("source", sourceIndex, "target", targetIndex));
				});
			});
		}
		return Map.of("nodes", nodes, "links", links);
	}

    public List<Person> findAll() {
        return personRepository.findAll();
    }
    public Optional<Person> findOneById(String id) {
        return personRepository.findOneById(id);
    }
    public Optional<Person> findOneByFullName(String fullName) {
        return personRepository.findOneByFullName(fullName);
    }

    public String getFullGraphvizSvg() {
        return graphvizProcessor.getSvg();
    }

    public Person findOneWithMostAncestors() {
        // Neo4jRepository default methods fetch nodes with full depth,
        // while custom-query methods are not. This is workaround.
        return
            personRepository.findById(personRepository.findIdOfOneWithMostAncestors()).get();
        
    }
    
}
