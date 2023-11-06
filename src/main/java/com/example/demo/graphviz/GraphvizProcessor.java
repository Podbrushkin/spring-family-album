package com.example.demo.graphviz;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.demo.model.Person;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizJdkEngine;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.LinkTarget;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

@Component
public class GraphvizProcessor {
    private static Logger log = LoggerFactory.getLogger(GraphvizProcessor.class.getName());
    private static MutableGraph fullGraph;
    private static Path baseDir;
    private static String fullSvg;


    // private PersonConverter personConverter = new PersonConverter();


    public String getSvg() {
        log.trace("Asked for SVG for graph of {} nodes.",fullGraph.nodes().size());
        Graphviz.useEngine(new GraphvizV8Engine(), new GraphvizJdkEngine());
        if (fullSvg == null)
        fullSvg =
            Graphviz.fromGraph(fullGraph)
                .width(800)
                // .height(100)
                .basedir(baseDir.toFile())
                .render(Format.SVG).toString();
        return fullSvg;
    }
    public List<Person> getPeople(Path graphvizTreePath) {
        if (Files.exists(graphvizTreePath)) {
            try {
                var dot = Files.readString(graphvizTreePath);
                baseDir = graphvizTreePath.getParent();
                return getPeople(dot);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    public List<Person> getPeople(String dot) {
        
        List<Person> personDots = new GraphvizProcessor().crawlDot(dot);
        return personDots;
    }
    private void describeMutableGraph(MutableGraph g) {
        log.trace("links({}): ",g.links().size());
        for (var l : g.links()) {
            log.trace(l.toString());
        }
        log.trace("edges({}): ",g.edges().size());
        List<Link> edges = new ArrayList<>(g.edges());
        edges = edges
            .stream()
            .sorted(
                Comparator.comparing(p -> p.from().toString(), 
                    Comparator.nullsFirst(Comparator.naturalOrder()))
                )
            .toList();
        for (var l : edges) {
            // System.out.printf("%s\n===>>>\t%s%n%n",l.from(), l.to());

            // System.out.println("FROM");
            System.out.println("l.from()="+l.from());
            System.out.println("l.from().name()="+l.from().name());

            // System.out.println("TO");
            System.out.println("l.to()="+l.to());
            System.out.println("l.to().name()="+l.to().name());


            
        }
    }

    private List<Person> crawlDot(String dot) {
        try {
            dot = removeComments(dot);
            MutableGraph g = new Parser().read(dot);
            GraphvizProcessor.fullGraph = g;
            describeMutableGraph(g);
            List<Person> people = new ArrayList<>();
            createPersonObjsFromRootNodesAndTheirLinks(people,g);
            createPersonObjsFromRootGraphs(people, g);
            // g.nodes().forEach(n -> System.out.println(n.name()));
            
            createHasChildRels(g, people);
            return people;
        
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void createPersonObjsFromRootNodesAndTheirLinks(List<Person> people, MutableGraph g) {
        

        for (var n : g.nodes()) {
            String name = n.name().value();
            String label = n.get("label") == null ? null : n.get("label").toString();
            if (name.endsWith("Img") || (name.endsWith("Desc")))
                continue;
            
            // skip mentioned-before-declared nodes but not literals
            // if ((!name.matches("(?i).*[а-я].*")) && (label == null) ) {
            if ((name.matches("\\w+")) && (label == null) ) {
                log.trace("Skipping node with id="+name);
                var links = n.links();
                if (links.size() != 0) {
                    System.out.println("But it had children!");
                    var person = findPersonByNameOrLabel(people, name);
                    System.out.println("Sure it didn't exist? "+person);
                    createPersonObjectsFromLinks(people, links);
                    // throw new IllegalStateException();
                }
                continue;
            }
            
            
            System.out.println("---");
            System.out.printf("[name=%s label=%s]%n", name, label);

            Person person = null;
            
            /* person = findPersonByIdOrName(people, name);
            if (person == null)
                person = findPersonByIdOrName(people, label); */
            
            /* if ((label != null) && (!label.isBlank())) {
                person = findPersonByIdOrName(people, label);
            } else 
            if (!name.isBlank())
                person = findPersonByIdOrName(people, label); */
            
            /* if (person != null) {
                System.out.println("------------------------");
                System.out.println(person.toString() + " found." );
                person.fillDataIfNotAlready(name,label);
            } */
            String lookFor = name == null ? label : name;
            person = findPersonByNameOrLabel(people, lookFor);
            
            if (person == null) {
                person = createPersonFrom(name,label);
                people.add(person);
            }
            

            var links = n.links();
            createPersonObjectsFromLinks(people,links);

            
            
            // System.out.printf("Links to: [%s]%n", linksStr);

            // System.out.println("---");
        }
        
        // return people;
    }

    private void createPersonObjectsFromLinks(List<Person> people, List<Link> links) {
        if (links.size() == 0) 
            return;

        for (Link l : links) {
            
            String child = l.to().toString();
            System.out.printf("We should check literal Persons from child block=%s...%n",child);

            String[] children = extractChildrenFromLink(child);
            Person childObj = null;
            for (var childName : children) {
                if (childName.matches("(?i).*[а-я].*")) {
                    System.out.println("We should find/create Person with name="+childName);
                    childObj = findPersonByNameOrLabel(people, childName);
                    if (childObj == null) {
                        childObj = createPersonFrom(childName,null);
                        people.add(childObj);
                    }
                }
            }
        }

    }

    private void createPersonObjsFromRootGraphs(List<Person> people, MutableGraph g) {
        log.trace("Apart from nodes, root MutableGraph also has graphs()...");
        /* for (var subgraphOrParentsUnion : g.graphs()) {
            log.trace("{} {} {} {}", subgraphOrParentsUnion.edges(), subgraphOrParentsUnion.isCluster(), subgraphOrParentsUnion.isStrict(),subgraphOrParentsUnion);
        } */
        var map = 
            g.graphs()
                .stream()
                .collect(
                    Collectors.groupingBy(gr -> gr.edges().size())
                    );
        
        
        List<MutableGraph> subgraphs = map.get(0);
        List<MutableGraph> parentsUnions = map.get(1);
        if (subgraphs == null) subgraphs = List.of();
        if (parentsUnions == null) parentsUnions = List.of();
        map = null;
        int subgraphsCount = subgraphs == null ? 0 : subgraphs.size();
        int parentsUnionsCount = parentsUnions == null ? 0 : parentsUnions.size();
        
        String msg = "There are {} subgraphs and {} parentsUnions, "+
            "[spouse] and [hasChild] rels should be extracted from them";
        log.trace(msg, subgraphsCount, parentsUnionsCount);

        

        
        log.trace("Extracting from parent blocks...");
        for (MutableGraph parents : parentsUnions) {
            
            var parentsArr = extractChildrenFromLink(parents.toString());
            log.trace("Parents: {}", Arrays.toString(parentsArr));

            if (parentsArr.length != 2) 
                throw new IllegalStateException("Only two parents are allowed: "+ parents);
            
            var parent1 =
                    findPersonByNameOrLabel(people, parentsArr[0]);
            var parent2 =
                    findPersonByNameOrLabel(people, parentsArr[1]);
            
            LinkTarget childrenBlock = parents.edges().iterator().next().to();
            
            if (parents.edges().size() != 1) 
                throw new IllegalStateException();
            
            // var childrenBlock2 = childrenBlock.linkTo().to().toString();
            // log.trace("childrenBlock also has childrenBlock: {}", childrenBlock2);
            
            // log.trace("edges: {}",tmp.to().toString());
            var childrenArr = extractChildrenFromLink(childrenBlock.toString());
            
            for (var childStr : childrenArr) {
                var childObj = findPersonByNameOrLabel(people, childStr);
                if (childObj == null) {
                    childObj = createPersonFrom(childStr, null );
                    people.add(childObj);
                    // throw new IllegalStateException("Child not found by this: '"+ childStr+"'");
                }
                boolean bothAdded = 
                    parent1.getChildren().add(childObj) && 
                    parent2.getChildren().add(childObj);
                if (!bothAdded)
                    throw new IllegalStateException("One of parents already had child: "+ childObj);
            }
        }
        log.trace("Extracting from subgraphs...");
        for (MutableGraph subgraph : subgraphs) {
            // var coupleArr = extractChildrenFromLink(subgraph.toString());
            // log.trace("Couple: {}", Arrays.toString(coupleArr));
            List<Person> coupleObjs = new ArrayList<>();
            createPersonObjsFromRootNodesAndTheirLinks(coupleObjs,subgraph);
            
            log.trace("People found in subgraph: {}, "+
                "these objs may not already exist.", coupleObjs);
            
            

            if (coupleObjs.size() == 2) {
                Person[] couple = new Person[2];

                for (int i = 0; (i < 2) && (coupleObjs.size() == 2); i++) {
                    
                    var personFromCouple = coupleObjs.get(i);
                    var alreadyExistedPerson = 
                        findPersonByNameOrLabel(people, personFromCouple.getFullName());
                    
                    if (alreadyExistedPerson == null) {
                        couple[i] = personFromCouple;
                        people.add(personFromCouple);
                    } else 
                        couple[i] = alreadyExistedPerson;
                    
                    
                }
                couple[0].setSpouse(couple[1]);
                // couple[1].setSpouse(couple[0]);
            }
            else if (coupleObjs.size() == 1) {
                var personFromCouple = coupleObjs.get(0);
                var alreadyExistedPerson = 
                    findPersonByNameOrLabel(people, personFromCouple.getFullName());
                
                if (alreadyExistedPerson == null) {
                    people.add(personFromCouple);
                } 
                // else  people.add(alreadyExistedPerson);
            }
            /* if (coupleObjs.size() == 2) {
                log.trace("Setting spouse relationship for them...");
                coupleObjs.get(0).setSpouse(coupleObjs.get(1));
            } */
            // people.addAll(coupleObjs);
        }
    }
        
    private void createHasChildRels(MutableGraph g, List<Person> people) {
        
        
        /* Map<Boolean,List<Link>> map = 
            g.edges()
                .stream()
                .collect(
                    Collectors.groupingBy(link -> link.getName() == null)
                    ); */
        
        log.trace("Processing...");
        for (var edge : g.edges()) {
            if (edge.from().name() == null)
                throw new IllegalStateException();
            if (edge.to().name() == null)
                throw new IllegalStateException();

            String[] parentsArr = null;
            if (edge.from().name().toString().isBlank())
                parentsArr = extractChildrenFromLink(edge.from().toString());
            else 
                parentsArr = extractChildrenFromLink(edge.from().name().toString());

            String[] childrenArr = null;
            if (edge.to().name().toString().isBlank())
                childrenArr = extractChildrenFromLink(edge.to().toString());
            else 
                childrenArr = extractChildrenFromLink(edge.to().name().toString());
            
            System.out.println("---------------");
            System.out.printf("%s -> %s%n",Arrays.toString(parentsArr), Arrays.toString(childrenArr));
            System.out.println("---------------");

            for (String parent : parentsArr) {
                var parentObj = findPersonByNameOrLabel(people, parent);
                for (String child : childrenArr) {
                    var childObj = findPersonByNameOrLabel(people, child);
                    parentObj.getChildren().add(childObj);
                }
            }
            // log.trace(parents+"<<<<<<<<<<<<<<<");
            // var parents = edge.from().name() == null ? edge.from() : edge.from().name();
            // log.trace(parentsArr+"<<<<<<<<<<<<<<<");

            // System.out.println("l.from()="+edge.from());
            // System.out.println("l.from().name()="+edge.from().name());
            
                // throw new IllegalStateException();
            
            // System.out.println("TO");
            // System.out.println("l.to()="+edge.to());
            // System.out.println("l.to().name()="+edge.to().name());
            // log.trace(">>>>>>>>>>>>>>>>"+Arrays.toString(parentsArr)+"<<<<<<<<<<<<<<<");

        }
    }



    private void createHasChildRelsOld(MutableGraph g, List<Person> people) {
        for (var n : g.nodes()) {
            var links = n.links();
            if (links.size() == 0) continue;

            String name = n.name().value();
            // String label = n.get("label") == null ? null : n.get("label").toString();

            Person parent = findPersonByNameOrLabel(people, name);

            for (Link l : links) {
                String child = l.to().toString();

                child = child.replaceAll("::", "");

                var childObj = findPersonByNameOrLabel(people, child);
                if (childObj != null) {
                    // System.out.println("Found child: " + childObj);
                    parent.getChildren().add(childObj);
                }
                else {
                    
                    
                    // System.out.printf("Didn't found child by this: '%s'%n", child);
                   
                    // System.out.println(Arrays.toString(children));
                    String[] children = extractChildrenFromLink(child);
                    for (var childName : children) {
                        childObj = findPersonByNameOrLabel(people, childName);
                        if (childObj != null) {
                            System.out.println("Found child:) " + childObj);
                            parent.getChildren().add(childObj);
                        } else {
                            var msg = String.format("Havent found childName=%s! ",childName);
                            msg += "At this point all Person objs should be created.";
                            System.out.println(msg);
                            throw new IllegalStateException(msg);
                        }
                    }

                }
            }
        }
    }

    public String[] extractChildrenFromLink(String childrenBlock) {
        // log.trace(String.format("children block recieved: '%s'",childrenBlock));
        if (childrenBlock.isBlank())
            return new String[0];
        else if (!childrenBlock.contains("{")) {
            String onlyChild = childrenBlock.replaceAll("::", "");
            log.trace(String.format("children block has only child: '%s'",onlyChild));
            return new String[] {onlyChild};
        }
        
        /* childrenBlock = childrenBlock.replaceAll(".*digraph \\{\\s*", "")
            .replaceAll("\\s*}\\s*", "")
            .replaceAll("[\r\n]+", "")
            .replaceAll("\\s*\"\"\\s*", "\"\"")
            ; */

        childrenBlock = childrenBlock.replaceAll("[\r\n]+", "")
            .replaceAll(".*digraph.*?\\{\\s*", "")
            .replaceAll("\\s*\\}\\s*$", "");
        // log.trace(String.format("children block interm: '%s'",childrenBlock));
        childrenBlock = 
            childrenBlock.replaceAll("\"\\s*\"", "\n")
            .replaceAll("\"", "");
        // log.trace(String.format("children block interm: '%s'",childrenBlock));

        // childrenBlock = childrenBlock.substring(1, childrenBlock.length()-1);
        String[] children = childrenBlock.split("\n");
        log.trace(String.format("children block had these els(%s): '%s'",children.length,Arrays.toString(children)));
        return children;
    }

    private Person findPersonByNameOrLabel(List<Person> people, String nameOrLabel) {
        if (nameOrLabel == null) return null;
        // System.out.println("Looking for... "+idOrName);
        // System.out.println(people.toString().contains(nameOrLabel)); 
        var personFound = people.stream()
            .filter(p -> {
                // System.out.printf("%s == %s : %s", p.dotId, idOrName, p.dotId==);
                return 
                    // (p.dotId == idOrName)||(p.fullname == idOrName);
                    (nameOrLabel.equals(p.getDotId())) ||
                    (nameOrLabel.equals(p.getFullName())) ||
                    (nodeLabelToFullName(nameOrLabel).equals(p.getFullName()))
                    ;
            })
            .findAny()
            .orElse(null);
        // System.out.println("Found this one: " + personFound);
        return personFound;
    }

    // name and label should be converted to dotId and fullName
    private Person createPersonFrom(String name, String label) {
        log.trace(String.format("Creating Person from: %s %s",name,label));
        var msg = String.format("Wrong data to create Person: %s %s. ",name,label);
        if (name == null) {
            msg += ".name is never null in graphviz nodes.";
            throw new IllegalStateException(msg);
        }
        String dotId = null;
        String fullName = null;
        // if ((label == null) && (!name.matches("(?i)[\\w\\d]+"))) {
        if (label == null) {
            dotId = null;
            fullName = nodeLabelToFullName(name);
            label = name;   // its for birthday only
            
        } else
        if(name.matches("[\\w]+")) {
            dotId = name;
            fullName = nodeLabelToFullName(label);
        }
        else {
            throw new IllegalStateException(msg);
        }
        
        var person = new Person(dotId, fullName);
        person.setBirthday(getBirthdayFromLabel(label));
        return person;
    }

    private String nodeLabelToFullName(String label) {
        int endOfFullname = label.indexOf("\\n");
        if (endOfFullname == -1) endOfFullname = label.length();
        var fullname = label.substring(0, endOfFullname);
        return fullname;
    }

    
    private LocalDate getBirthdayFromLabel(String label) {
        if (label == null) return null;
        Pattern p = Pattern.compile(".*\\\\n(\\d{2})\\.(\\d{2}).(\\d{4}).*");
        Matcher m = p.matcher(label);
        LocalDate birthday = null;
        if (m.matches()) {
            birthday = LocalDate.parse(m.group(3)+"-"+m.group(2)+"-"+m.group(1));
        } 
        return birthday;
    }


    private String removeComments(String input) {
        String result = input;
        result = result.replaceAll("(?m)#.*$", "")
            .replaceAll("(?s)/\\*.*?\\*/", "");
        return result;
    }

    private void printPeople(List<Person> people) {
        System.out.println("---All people:---");
        people.stream()
            .sorted(
                Comparator.comparing(p -> p.getFullName(), 
                    Comparator.nullsFirst(Comparator.naturalOrder()))
                )
            .forEach(System.out::println);
        System.out.println("---             ---");
    }
}
