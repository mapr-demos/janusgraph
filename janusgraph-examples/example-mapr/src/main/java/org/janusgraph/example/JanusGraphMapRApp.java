package org.janusgraph.example;

import com.github.javafaker.Faker;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.RelationType;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

public class JanusGraphMapRApp {

    private static final Logger log = LoggerFactory.getLogger(JanusGraphMapRApp.class);

    private static final int PERSON_NUM = 20;
    private static final LocalDate FOLLOWING_DATE_START = LocalDate.of(2005, 1, 1);
    private static final LocalDate FOLLOWING_DATE_END = LocalDate.of(2018, 4, 23);

    private static String propFileName;
    private static Graph graph;
    private static GraphTraversalSource g;

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            throw new IllegalArgumentException("Path to property file required as argument.");
        }

        propFileName = args[0];

        if (args.length > 1 && "drop".equalsIgnoreCase(args[1])) {
            openGraph();
            dropGraph();
            return;
        }

        // open and initialize the graph
        openGraph();

        // define the schema before loading data
        createSchema((JanusGraph) graph);

        // build the graph structure
        createElements();

        // read to see they were made
        readElements();

        // update some graph elements with changes
        updateElements();

        // read to see the changes were made
        readElements();

        // delete some graph elements
        deleteElements();

        // read to see the changes were made
        readElements();

        // close the graph
        closeGraph();
    }

    /**
     * Opens the graph instance. If the graph instance does not exist, a new
     * graph instance is initialized.
     */
    public static GraphTraversalSource openGraph() throws ConfigurationException {
        log.info("============================ Opening graph ============================");
        Configuration conf = new PropertiesConfiguration(propFileName);
        graph = GraphFactory.open(conf);
        g = graph.traversal();
        return g;
    }

    /**
     * Closes the graph instance.
     */
    public static void closeGraph() throws Exception {
        log.info("============================ Closing graph ============================");
        try {
            if (g != null) {
                g.close();
            }
            if (graph != null) {
                graph.close();
            }
        } finally {
            g = null;
            graph = null;
        }
    }

    /**
     * Drops the graph instance.
     */
    public static void dropGraph() throws Exception {
        if (graph != null) {
            JanusGraphFactory.drop((JanusGraph) graph);
        }
    }

    /**
     * Creates the graph schema.
     */
    public static void createSchema(JanusGraph graph) {
        final JanusGraphManagement management = graph.openManagement();
        try {
            // naive check if the schema was previously created
            if (management.getRelationTypes(RelationType.class).iterator().hasNext()) {
                management.rollback();
                return;
            }

            log.info("============================ Creating schema ============================");
            createProperties(management);
            createVertexLabels(management);
            createEdgeLabels(management);
            management.commit();
        } catch (Exception e) {
            management.rollback();
        }
    }

    /**
     * Creates the vertex labels.
     */
    protected static void createVertexLabels(final JanusGraphManagement management) {
        management.makeVertexLabel("person").make();
    }

    /**
     * Creates the edge labels.
     */
    protected static void createEdgeLabels(final JanusGraphManagement management) {
        management.makeEdgeLabel("following").signature(management.getPropertyKey("timestamp")).make();
        management.makeEdgeLabel("followedBy").signature(management.getPropertyKey("timestamp")).make();
    }

    /**
     * Creates the properties for vertices, edges, and meta-properties.
     */
    protected static void createProperties(final JanusGraphManagement management) {
        management.makePropertyKey("name").dataType(String.class).make();
        management.makePropertyKey("age").dataType(Integer.class).make();
        management.makePropertyKey("date").dataType(String.class).make();
    }

    /**
     * Adds the vertices, edges, and properties to the graph.
     */
    public static void createElements() {
        try {

            // check if the graph was previously created
            if (g.V().hasNext()) {
                g.tx().rollback();
                return;
            }
            log.info("============================ Creating elements ============================");


            Faker faker = new Faker();
            Random random = new Random();
            List<Vertex> vertices = new ArrayList<>();
            for (int i = 0; i < PERSON_NUM; i++) {
                String name = faker.name().fullName();
                Integer age = 15 + random.nextInt(50);
                Vertex vertex = g.addV("person").property("name", name).property("age", age).next();
                vertices.add(vertex);
            }

            for (Vertex person : vertices) {

                // Random number of subscriptions
                int subscriptions = random.nextInt(PERSON_NUM / 2);
                Set<Integer> indicesOfFollowing = new HashSet<>();
                while (indicesOfFollowing.size() < subscriptions) {

                    int randomIndex = random.nextInt(PERSON_NUM);
                    Vertex randomPerson = vertices.get(randomIndex);

                    // Follow random person
                    if (!person.id().equals(randomPerson.id()) && !indicesOfFollowing.contains(randomIndex)) {

                        long randomDays = (long) (FOLLOWING_DATE_START.toEpochDay() + random.nextDouble() *
                            (FOLLOWING_DATE_END.toEpochDay() - FOLLOWING_DATE_START.toEpochDay()));
                        LocalDate randomDate = LocalDate.ofEpochDay(randomDays);

                        g.V(person).as("a").V(randomPerson).addE("following")
                            .property("date", randomDate.toString()).from("a").next();

                        indicesOfFollowing.add(randomIndex);
                    }
                }
            }

            g.tx().commit();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            g.tx().rollback();
        }
    }

    /**
     * Runs some traversal queries to get data from the graph.
     */
    public static void readElements() {
        try {
            if (g == null) {
                return;
            }

            log.info("============================ Reading elements ============================");

            // Iterate over all vertices
            GraphTraversal<Vertex, Vertex> traversal = g.V();
            int counter = 0;
            while (traversal.hasNext()) {

                Vertex vertex = traversal.next();
                String personName = vertex.value("name");
                Integer personAge = vertex.value("age");
                log.info("{})", counter);
                log.info("Person: {}, {}", personName, personAge);
                log.info("Vertex property list: {}", IteratorUtils.list(vertex.values()));

                // Get 'followedBy' edges
                Iterator<Edge> followedByIterator = vertex.edges(Direction.IN, "following");
                if (followedByIterator.hasNext()) {
                    log.info("{} followed by:", personName);
                    followedByIterator.forEachRemaining(e -> {
                        String followerName = e.outVertex().value("name");
                        String followedSince = e.value("date");

                        log.info("\t{} since {}", followerName, followedSince);
                    });

                    // Count 'followedBy' edges
                    log.info("Total followers: {}", IteratorUtils.count(g.V(vertex).inE("following")));
                }

                // Get 'following' edges
                Iterator<Edge> followingIterator = vertex.edges(Direction.OUT, "following");
                if (followingIterator.hasNext()) {
                    log.info("{} follows:", personName);
                    followingIterator.forEachRemaining(e -> {
                        String followingName = e.inVertex().value("name");
                        String followingSince = e.value("date");

                        log.info("\t{} since {}", followingName, followingSince);
                    });

                    // Count 'following' edges
                    log.info("Total following: {}", IteratorUtils.count(g.V(vertex).outE("following")));
                }

                log.info("********************************\n");
                counter++;
            }


        } finally {
            // the default behavior automatically starts a transaction for
            // any graph interaction, so it is best to finish the transaction
            // even for read-only graph query operations
            g.tx().rollback();
        }
    }

    /**
     * Makes an update to the existing graph structure. Does not create any
     * new vertices or edges.
     */
    public static void updateElements() {
        try {
            if (g == null) {
                return;
            }
            log.info("============================ Updating elements ============================");

            final long ts = System.currentTimeMillis();
            GraphTraversal<Vertex, Vertex> toUpdate = g.V().sample(1);
            toUpdate.property("timestamp", ts);
            log.info("Adding 'timestamp' field to '{}' vertex", toUpdate.values("name").toStream().findAny().get());
            toUpdate.iterate();

            g.tx().commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            g.tx().rollback();
        }
    }

    /**
     * Deletes elements from the graph structure. When a vertex is deleted,
     * its incident edges are also deleted.
     */
    public static void deleteElements() {
        try {
            if (g == null) {
                return;
            }
            log.info("============================ Deleting elements ============================");

            Vertex toDelete = g.V().sample(1).next();
            String name = toDelete.value("name");
            log.info("Deleting '{}' vertex", name);
            g.V(toDelete).drop().iterate();

            g.tx().commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            g.tx().rollback();
        }
    }
}
