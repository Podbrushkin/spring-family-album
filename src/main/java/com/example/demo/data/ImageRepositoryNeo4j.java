package com.example.demo.data;

import java.util.Collection;
import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.model.Image;
import com.example.demo.model.Person;

public interface ImageRepositoryNeo4j extends Neo4jRepository<Image, String>, ImageRepositoryNeo4jFragment {
    @Query("""
            MATCH (i:Image), (p:Person)
            WHERE 
                toString(p.birthday) IN i.tags
            OR 
                p.fullName IN i.tags
            MERGE (i)-[:DEPICTS]->(p)
            RETURN count(*)
            """)
    int createImageDepictsPersonRels();

    @Query("MATCH (Image) -[r:DEPICTS]->(Person) RETURN count(r)")
    int countImageDepictsPersonRels();

    Image findOneByImHashOrDgkmHash(String mgckHash, String dgkmHash);

    @Query("""
        MATCH (i:Image) -[:DEPICTS] -> (p:Person)
        WHERE p IN $people
        RETURN i
        """)
    List<Image> findByPeopleDepictedContainsAll(@Param("people") Collection<Person> people);

    @Query("""
        MATCH (i:Image) -[:DEPICTS] -> (p:Person)
        WHERE toString(p.birthday) IN $bdays
        RETURN i
        """)
    List<Image> findByPeopleDepictedContainsAllBdays(@Param("bdays") Collection<String> bdays);

    /* @Query("""
        MATCH (i:Image)
        WHERE 
            all(x IN i.tags WHERE x IN $tags)
        RETURN i
        """) */
    // what if Person doesn't have bday nor dotId?
    @Query("""
        MATCH (p:Person)
        WHERE 
            (p.birthday IS NOT NULL
            AND
            toString(p.birthday) IN $tags)
            OR
            p.fullName IN $tags
        WITH collect(p) AS people
        MATCH (i:Image)-[:DEPICTS]->(p)
        WHERE ALL(p IN people WHERE (i)-[:DEPICTS]->(p))
        RETURN DISTINCT i
        """)
    List<Image> findByTagsContainsAll(@Param("tags") Collection<String> tags);

    /* @Query("""
        MATCH (i:Image) -[:DEPICTS] -> (p:Person)
        WHERE p IS $person
        RETURN p,count(i)
        """)
    public Map<Person,Integer> countImagesForPersons(@Param("persons") Collection<Person> persons); */
    
    @Query("""
        MATCH (i:Image) -[:DEPICTS] -> (p:Person {fullName: $person})
        RETURN count(i)
        """)
    // TODO: personFullname is not unique, use smth else
    public Integer countByPeopleContains(@Param("person") String personFullName);

    @Query("""
        MATCH (i:Image) -[r:DEPICTS] -> (p:Person)
        WITH size(i.tags) AS tagsCount, size(collect(p)) AS people, i
        WHERE tagsCount <> people
        RETURN i
        """)
    List<Image> findWhereTagsCountNotSameAsDepictsCount();
}
