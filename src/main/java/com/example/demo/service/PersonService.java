package com.example.demo.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.data.ImageRepositoryNeo4j;
import com.example.demo.data.PersonRepository;
import com.example.demo.graphviz.GraphvizProcessor;
import com.example.demo.model.Person;
import com.example.demo.model.PersonDto;

@Service
public class PersonService {
    private static Logger log = LoggerFactory.getLogger(PersonService.class);
    private PersonRepository personRepository;
    private ImageRepositoryNeo4j imageRepositoryNeo4j;
    private Collection<PersonDto> allDepictedWithCountsDto;
    @Autowired
    private GraphvizProcessor graphvizProcessor;
    @Autowired
    private GraphDatabaseService graphDatabaseService;

    public PersonService(PersonRepository personRepository, ImageRepositoryNeo4j imageRepositoryNeo4j) {
        this.imageRepositoryNeo4j = imageRepositoryNeo4j;
        this.personRepository = personRepository;
    }

    public String getPersonGraphJson() {
        String cypher = """
            MATCH (nod:Person)
            MATCH ()-[rels:HAS_CHILD]->()
            WITH collect(DISTINCT nod) as a, collect(DISTINCT rels) as b
            CALL apoc.export.json.data(a, b, null, { jsonFormat: 'JSON', stream: true})
            YIELD data
            RETURN data
            """;
        try (var tx = graphDatabaseService.beginTx()) {
            var result = tx.execute(cypher);
            String json = (String) result.next().values().iterator().next();
            return json;
        }
    }

    public Collection<Person> findAllDepictedByAtLeastOneImage() {
        return personRepository.findAllDepictedByAtLeastOneImage();
    }

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

   

    public Map<String, List<Object>> fetchPersonGraph() {
        var people = this.findAll();
        return buildJsonGraphOnHasChildRel(people);
    }

    public Map<String, List<Object>> fetchPersonAncestorsGraph(Person person) {
        // var people = personRepository.findAllAncestorsOf(person.getId());
        var people = flattenChildren(person).toList();
        log.trace("Found these ancestors of {}: \n{}",person, people);
        // people.add(person);

        

        return buildJsonGraphOnHasChildRel(people);
    }

    private static Stream<Person> flattenChildren(Person person) {
        return Stream.concat(
            Stream.of(person), 
            person.getChildren().stream().flatMap(PersonService::flattenChildren))
        .peek(System.out::println);
    }

    public Map<String, List<Object>> buildJsonGraphOnHasChildRel(List<Person> people) {
        var nodes = new ArrayList<>();
		var links = new ArrayList<>();

        
        for (var person : people) {
            var parent = Map.of("label", "person", 
                                "title", person.getFullName(),
                                "id",person.getId());

            // if (!nodes.contains(parent)) 
                nodes.add(parent);
            for (var child : person.getChildren()) {
                /* var childJson = Map.of("label", "person", 
                                        "title", child.getFullName(),
                                        "id",child.getId());
                if (!nodes.contains(childJson)) 
                    nodes.add(childJson); */
                
                var link = Map.of("source",person.getId(),
                    "target", child.getId());
                links.add(link);
            }
        }
        return Map.of("nodes", nodes, "links", links);
    }

    public List<Person> findAll() {
        return personRepository.findAll();
    }
    public Optional<Person> findOneById(String id) {
        return personRepository.findById(id);
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
        // https://graphaware.com/neo4j/2016/04/06/mapping-query-entities-sdn.html
        // https://neo4j.com/docs/ogm-manual/current/reference/#reference:session:loading-entities
        return
            personRepository.findById(personRepository.findIdOfOneWithMostAncestors()).get();
        
    }
    
}
