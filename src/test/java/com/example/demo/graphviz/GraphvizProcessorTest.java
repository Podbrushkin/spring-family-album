package com.example.demo.graphviz;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.example.demo.model.Person;

public class GraphvizProcessorTest {
    GraphvizProcessor graphvizProcessor = new GraphvizProcessor();
    @Test
    void testGetPeopleSubgraphCouple() {
        String dot = """
            digraph G {
                subgraph clusterSomeCouple {
                    vasya [label = "Василий Васильев" height=0.3]
                    masha [label = "Мария Ивановна" height=0.3]
                }
            }
            """;
        
        var p1 = new Person("vasya","Василий Васильев");
        var p2 = new Person("masha","Мария Ивановна");
        p1.setSpouse(p2);
        var peopleReference = new HashSet<Person>(List.of(p1,p2));
        var peopleList = graphvizProcessor.getPeople(dot);

        Assertions.assertNotNull(peopleList.get(0).getSpouse());
        Assertions.assertEquals(peopleList.get(0).getSpouse(),peopleList.get(1));
        Assertions.assertEquals(peopleList.get(1).getSpouse(),peopleList.get(0));
        

        var peopleSet = new HashSet<>(graphvizProcessor.getPeople(dot));
        Assertions.assertEquals(peopleReference, peopleSet);
    }
    @Test
    void testGetPeopleSubgraphSingle() {
        String dot = """
            digraph G {
                subgraph clusterSomeCouple {
                    vasya [label = "Василий Васильев" height=0.3]
                }
            }
            """;
        var p1 = new Person("vasya","Василий Васильев");
        var peopleList = graphvizProcessor.getPeople(dot);
        System.out.println(peopleList);
        Assertions.assertEquals(1, peopleList.size());
        Assertions.assertEquals(p1, peopleList.get(0));
    }

    @Test
    void testGetPeopleGraphLiteral() {
        String dot = """
            digraph G {
                subgraph clusterSomeCouple {
                    "Василий Васильев"
                }
            }
            """;
        var p1 = new Person(null,"Василий Васильев");
        var peopleList = graphvizProcessor.getPeople(dot);
        Assertions.assertEquals(1, peopleList.size());
        Assertions.assertEquals(p1, peopleList.get(0));
    }

    @Test
    void testGetPeopleRootNode() {
        String dot = """
            digraph G {
                vasya [label = "Василий Васильев" height=0.3]
            }
            """;
        var p1 = new Person("vasya","Василий Васильев");
        var peopleList = graphvizProcessor.getPeople(dot);
        Assertions.assertEquals(1, peopleList.size());
        Assertions.assertEquals(p1, peopleList.get(0));
    }
    @Test
    void testGetPeopleRootLiteral() {
        String dot = """
            digraph G {
                "Василий Васильев"
            }
            """;
        var p1 = new Person(null,"Василий Васильев");
        var peopleList = graphvizProcessor.getPeople(dot);
        Assertions.assertEquals(1, peopleList.size());
        Assertions.assertEquals(p1, peopleList.get(0));
    }
    @Test
    void testGetPeopleNodeToLiteral() {
        String dot = """
            digraph G {
                masha [label = "Мария Ивановна" height=0.3] 
                masha -> "Василий Васильев"
            }
            """;
        var p1 = new Person(null,"Василий Васильев");
        var p2 = new Person("masha","Мария Ивановна");
        p2.getChildren().add(p1);
        var peopleExpected = new HashSet<Person>(List.of(p1,p2));
        var peopleActual = new HashSet<Person>(graphvizProcessor.getPeople(dot));
        Assertions.assertEquals(peopleExpected, peopleActual);
        var p2actual =
        peopleActual.stream()
            .filter(p->p.getChildren().size()==1)
            .findFirst()
            .orElseThrow();
        
        Assertions.assertTrue(p2actual.getChildren().contains(p1));
    }
    @Test
    void testGetPeopleNodeToNode() {
        String dot = """
            digraph G {
                masha [label = "Мария Ивановна" height=0.3] 
                vasya [label = "Василий Васильев" height=0.3]
                masha -> vasya
            }
            """;
        var p1 = new Person("vasya","Василий Васильев");
        var p2 = new Person("masha","Мария Ивановна");
        p2.getChildren().add(p1);
        var peopleExpected = new HashSet<Person>(List.of(p1,p2));
        var peopleActual = new HashSet<Person>(graphvizProcessor.getPeople(dot));
        Assertions.assertEquals(peopleExpected, peopleActual);
        var p2actual =
        peopleActual.stream()
            .filter(p->p.getChildren().size()==1)
            .findFirst()
            .orElseThrow();
        
        Assertions.assertTrue(p2actual.getChildren().contains(p1));
    }
    @Test
    void testGetPeopleBlockToNode() {
        String dot = """
            digraph G {
                masha [label = "Мария Ивановна" height=0.3] 
                vasya [label = "Василий Васильев" height=0.3]
                serg [label = "Сергей Сергеев" height=0.3]
                
                { masha vasya } -> serg
            }
            """;
        var p1 = new Person("vasya","Василий Васильев");
        var p2 = new Person("masha","Мария Ивановна");
        var p3 = new Person("serg","Сергей Сергеев");
        p1.getChildren().add(p3);
        p2.getChildren().add(p3);
        var peopleExpected = new HashSet<Person>(List.of(p1,p2,p3));
        var peopleActual = new HashSet<Person>(graphvizProcessor.getPeople(dot));
        Assertions.assertEquals(peopleExpected, peopleActual);
        
        var parents = 
        peopleActual.stream()
            .filter(p->p.getChildren().size()==1)
            .toList();
        
        Assertions.assertEquals(2,parents.size());
        
        for (var p : parents) {
            Assertions.assertTrue(p.getChildren().contains(p3));
            // Assertions.assertEquals(p3,p.getChildren().iterator().next());
        }
    }
    @Test
    void testGetPeopleBlockToLiteral() {
        String dot = """
            digraph G {
                masha [label = "Мария Ивановна" height=0.3] 
                vasya [label = "Василий Васильев" height=0.3]
                
                { masha vasya } -> "Сергей Сергеев"
            }
            """;
        var p1 = new Person("vasya","Василий Васильев");
        var p2 = new Person("masha","Мария Ивановна");
        var p3 = new Person(null,"Сергей Сергеев");
        p1.getChildren().add(p3);
        p2.getChildren().add(p3);
        var peopleExpected = new HashSet<Person>(List.of(p1,p2,p3));
        var peopleActual = new HashSet<Person>(graphvizProcessor.getPeople(dot));
        Assertions.assertEquals(peopleExpected, peopleActual);
        
        var parents = 
        peopleActual.stream()
            .filter(p->p.getChildren().size()==1)
            .toList();
        
        Assertions.assertEquals(2,parents.size());
        
        for (var p : parents) {
            Assertions.assertTrue(p.getChildren().contains(p3));
            // Assertions.assertEquals(p3,p.getChildren().iterator().next());
        }
    }
    @Test
    void testGetPeopleBlockWithLiteralToNode() {
        String dot = """
            digraph G {
                masha [label = "Мария Ивановна" height=0.3] 
                serg [label = "Сергей Сергеев" height=0.3]
                { masha "Василий Васильев" } -> serg
            }
            """;
        var p1 = new Person(null,"Василий Васильев");
        var p2 = new Person("masha","Мария Ивановна");
        var p3 = new Person("serg","Сергей Сергеев");
        p1.getChildren().add(p3);
        p2.getChildren().add(p3);
        var peopleExpected = new HashSet<Person>(List.of(p1,p2,p3));
        var peopleActual = new HashSet<Person>(graphvizProcessor.getPeople(dot));
        Assertions.assertEquals(peopleExpected, peopleActual);
        
        var parents = 
        peopleActual.stream()
            .filter(p->p.getChildren().size()==1)
            .toList();
        
        Assertions.assertEquals(2,parents.size());
        
        for (var p : parents) {
            Assertions.assertTrue(p.getChildren().contains(p3));
            // Assertions.assertEquals(p3,p.getChildren().iterator().next());
        }
    }

    @Test
    void testGetPeopleBlockToBlock() {
        String dot = """
            digraph G {
                masha [label = "Мария Ивановна" height=0.3] 
                serg [label = "Сергей Сергеев" height=0.3]
                ekat [label = "Екатерина Екатериновна" height=0.3]
                { masha "Василий Васильев" } -> { serg ekat }
            }
            """;
        var p1 = new Person(null,"Василий Васильев");
        var p2 = new Person("masha","Мария Ивановна");
        var p3 = new Person("serg","Сергей Сергеев");
        var p4 = new Person("ekat","Екатерина Екатериновна");
        p1.getChildren().add(p3);
        p2.getChildren().add(p3);
        p1.getChildren().add(p4);
        p2.getChildren().add(p4);
        var peopleExpected = new HashSet<Person>(List.of(p1,p2,p3,p4));
        var peopleActual = new HashSet<Person>(graphvizProcessor.getPeople(dot));
        Assertions.assertEquals(peopleExpected, peopleActual);
        
        var parents = 
        peopleActual.stream()
            .filter(p->p.getChildren().size()==2)
            .toList();
        
        Assertions.assertEquals(2,parents.size());
        
        for (var p : parents) {
            Assertions.assertTrue(p.getChildren().contains(p3));
            Assertions.assertTrue(p.getChildren().contains(p4));
            // Assertions.assertEquals(p3,p.getChildren().iterator().next());
        }
    }
    @Test
    void testGetPeopleBlockToNodeToBlock() {
        String dot = """
            digraph G {
                masha [label = "Мария Ивановна" height=0.3] 
                serg [label = "Сергей Сергеев" height=0.3]
                ekat [label = "Екатерина Екатериновна" height=0.3]
                ivan [label = "Иван Иванов" height=0.3]
                { masha "Василий Васильев" } -> serg -> { ekat ivan }
            }
            """;
        var p1 = new Person(null,"Василий Васильев");
        var p2 = new Person("masha","Мария Ивановна");
        var p3 = new Person("serg","Сергей Сергеев");
        var p4 = new Person("ekat","Екатерина Екатериновна");
        var p5 = new Person("ivan","Иван Иванов");
        p1.getChildren().add(p3);
        p2.getChildren().add(p3);
        p3.getChildren().add(p4);
        p3.getChildren().add(p5);
        var peopleExpected = new HashSet<Person>(List.of(p1,p2,p3,p4,p5));
        var peopleActual = new HashSet<Person>(graphvizProcessor.getPeople(dot));
        Assertions.assertEquals(peopleExpected, peopleActual);
        
        var parents = 
        peopleActual.stream()
            .filter(p->p.getChildren().size()==2)
            .toList();
        
        Assertions.assertEquals(1,parents.size());
        
        for (var p : parents) {
            Assertions.assertTrue(p.getChildren().contains(p4));
            Assertions.assertTrue(p.getChildren().contains(p5));
            // Assertions.assertEquals(p3,p.getChildren().iterator().next());
        }

        parents = 
        peopleActual.stream()
            .filter(p->p.getChildren().size()==1)
            .toList();
        
        Assertions.assertEquals(2,parents.size());
        
        for (var p : parents) {
            Assertions.assertTrue(p.getChildren().contains(p3));
        }
    }

}
