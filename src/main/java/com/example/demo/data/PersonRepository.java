package com.example.demo.data;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Person;

public interface PersonRepository extends Neo4jRepository<Person, String> {
    Optional<Person> findOneById(String id);
    Optional<Person> findOneByBirthday(LocalDate birthday);
    Optional<Person> findOneByBirthday(String birthday);

    List<Person> findAllByBirthdayIn(Collection<LocalDate> birthdays);

    List<Person> findAllByFullNameIn(Collection<String> names);

    @Query("MATCH (:Image)-[:DEPICTS]->(person:Person) RETURN DISTINCT person")
    List<Person> findAllDepictedByAtLeastOneImage();

    /* @Query("""
        MATCH (i:Image)-[:DEPICTS]->(person:Person)
        RETURN person,count(i)
        """)
    List<Map<Person,Integer>> findAllDepictedByAtLeastOneImageWithCounts(); */

    @Query("""
        MATCH (i:Image) -[:DEPICTS] -> (:Person {id: $personId})
        RETURN count(i)
        """)
    public Integer countImagesForPersonId(@Param("personId") String personId);
    // public Integer countImagesByPerson(Person person);
}
