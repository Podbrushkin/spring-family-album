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
    Optional<Person> findOneByFullName(String fullname);

    List<Person> findAllByBirthdayIn(Collection<LocalDate> birthdays);

    List<Person> findAllByFullNameIn(Collection<String> names);

    @Query("MATCH (:Image)-[:DEPICTS]->(person:Person) RETURN DISTINCT person")
    List<Person> findAllDepictedByAtLeastOneImage();

    @Query("""
        MATCH (p:Person)-[:HAS_CHILD*]->(ancestor:Person)
        WITH p, COLLECT(ancestor) AS ancestors
        ORDER BY size(ancestors) DESC
        LIMIT 1
        RETURN elementId(p)
    """)
    public String findIdOfOneWithMostAncestors();

    @Query("""
        MATCH (p:Person)-[:HAS_CHILD*]->(ancestor:Person)
        WHERE elementId(p) = $predecessor
        RETURN ancestor
    """)
    public List<Person> findAllAncestorsOf(@Param("predecessor") String descendantId);
}
