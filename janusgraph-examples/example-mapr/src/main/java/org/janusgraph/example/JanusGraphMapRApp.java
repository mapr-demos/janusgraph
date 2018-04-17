package org.janusgraph.example;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.RelationType;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public class JanusGraphMapRApp {

    private static final Logger log = LoggerFactory.getLogger(JanusGraphMapRApp.class);

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
        log.info("Opening graph");
        Configuration conf = new PropertiesConfiguration(propFileName);
        graph = GraphFactory.open(conf);
        g = graph.traversal();
        return g;
    }

    /**
     * Closes the graph instance.
     */
    public static void closeGraph() throws Exception {
        log.info("Closing graph");
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

            log.info("Creating schema");
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
            // naive check if the graph was previously created
            if (g.V().has("name", "saturn").hasNext()) {
                g.tx().rollback();
                return;
            }
            log.info("============================ Creating elements ============================");

            // see GraphOfTheGodsFactory.java

            final Vertex bob = g.addV("person").property("name", "Bob").property("age", 19).next();
            final Vertex mike = g.addV("person").property("name", "Mike").property("age", 25).next();
            final Vertex daniel = g.addV("person").property("name", "Daniel").property("age", 22).next();
            final Vertex john = g.addV("person").property("name", "John").property("age", 27).next();
            final Vertex chris = g.addV("person").property("name", "Chris").property("age", 24).next();

            g.V(bob).as("a").V(mike).addE("followedBy").property("date", "19-01-2015").from("a").next();
            g.V(bob).as("a").V(daniel).addE("followedBy").property("date", "16-05-2013").from("a").next();
            g.V(bob).as("a").V(daniel).addE("following").property("date", "17-05-2013").from("a").next();
            g.V(bob).as("a").V(john).addE("following").property("date", "28-11-2013").from("a").next();

            g.V(mike).as("a").V(bob).addE("following").property("date", "19-01-2015").from("a").next();

            g.V(daniel).as("a").V(bob).addE("following").property("date", "12-07-2017").from("a").next();
            g.V(daniel).as("a").V(bob).addE("followedBy").property("date", "13-08-2015").from("a").next();
            g.V(daniel).as("a").V(john).addE("following").property("date", "01-01-2018").from("a").next();

            g.V(john).as("a").V(bob).addE("followedBy").property("date", "27-07-2011").from("a").next();
            g.V(john).as("a").V(daniel).addE("followedBy").property("date", "25-10-2015").from("a").next();
            g.V(john).as("a").V(chris).addE("following").property("date", "17-03-2016").from("a").next();

            g.V(chris).as("a").V(john).addE("followedBy").property("date", "16-04-2017").from("a").next();

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
            readPersonElement("Bob");
            readPersonElement("Mike");
            readPersonElement("Daniel");
            readPersonElement("John");
            readPersonElement("Chris");

        } finally {
            // the default behavior automatically starts a transaction for
            // any graph interaction, so it is best to finish the transaction
            // even for read-only graph query operations
            g.tx().rollback();
        }
    }

    private static void readPersonElement(String name) {
        // look up vertex by name can use a composite index in JanusGraph
        final Optional<Map<Object, Object>> v = g.V().has("name", name).valueMap(true).tryNext();
        if (v.isPresent()) {
            log.info(v.get().toString());
        } else {
            log.warn("{} not found", name);
        }

        // look up an incident edge
        log.info("{}'s followers: {}", name, g.V().has("name", name).out("followedBy").values("name").dedup().toList());
        log.info("{} follows: {}", name, g.V().has("name", name).out("following").values("name").dedup().toList());
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
            g.V().has("name", "Bob").property("ts", ts).iterate();
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
            // note that this will succeed whether or not pluto exists
            g.V().has("name", "Bob").drop().iterate();
            g.tx().commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            g.tx().rollback();
        }
    }
}
