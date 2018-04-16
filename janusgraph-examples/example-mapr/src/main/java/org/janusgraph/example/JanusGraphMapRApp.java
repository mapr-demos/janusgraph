package org.janusgraph.example;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.Multiplicity;
import org.janusgraph.core.RelationType;
import org.janusgraph.core.attribute.Geoshape;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JanusGraphMapRApp {

    private static final Logger log = LoggerFactory.getLogger(JanusGraphMapRApp.class);

    private static String propFileName;
    private static Configuration conf;
    private static Graph graph;
    private static GraphTraversalSource g;

    public static void main(String[] args) {
        propFileName = (args != null && args.length > 0) ? args[0] : null;
        runApp();
    }

    /**
     * Run the entire application:
     * 1. Open and initialize the graph
     * 2. Define the schema
     * 3. Build the graph
     * 4. Run traversal queries to get data from the graph
     * 5. Make updates to the graph
     * 6. Close the graph
     */
    public static void runApp() {
        try {
            // open and initialize the graph
            openGraph();

            // define the schema before loading data
            createSchema((JanusGraph) graph);

            // build the graph structure
            createElements();
            // read to see they were made
            readElements();

            for (int i = 0; i < 3; i++) {
                try {
                    Thread.sleep((long) (Math.random() * 500) + 500);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
                // update some graph elements with changes
                updateElements();
                // read to see the changes were made
                readElements();
            }

            // delete some graph elements
            deleteElements();
            // read to see the changes were made
            readElements();

            // close the graph
            closeGraph();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Opens the graph instance. If the graph instance does not exist, a new
     * graph instance is initialized.
     */
    public static GraphTraversalSource openGraph() throws ConfigurationException {
        log.info("Opening graph");
        conf = new PropertiesConfiguration(propFileName);
        graph = GraphFactory.open(conf);
        g = graph.traversal();
        return g;
    }

    /**
     * Closes the graph instance.
     */
    public static void closeGraph() throws Exception {
        log.info("closing graph");
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
     * Drops the graph instance. The default implementation does nothing.
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
        management.makeVertexLabel("titan").make();
        management.makeVertexLabel("location").make();
        management.makeVertexLabel("god").make();
        management.makeVertexLabel("demigod").make();
        management.makeVertexLabel("human").make();
        management.makeVertexLabel("monster").make();
    }

    /**
     * Creates the edge labels.
     */
    protected static void createEdgeLabels(final JanusGraphManagement management) {
        management.makeEdgeLabel("father").multiplicity(Multiplicity.MANY2ONE).make();
        management.makeEdgeLabel("mother").multiplicity(Multiplicity.MANY2ONE).make();
        management.makeEdgeLabel("lives").signature(management.getPropertyKey("reason")).make();
        management.makeEdgeLabel("pet").make();
        management.makeEdgeLabel("brother").make();
        management.makeEdgeLabel("battled").make();
    }

    /**
     * Creates the properties for vertices, edges, and meta-properties.
     */
    protected static void createProperties(final JanusGraphManagement management) {
        management.makePropertyKey("name").dataType(String.class).make();
        management.makePropertyKey("age").dataType(Integer.class).make();
        management.makePropertyKey("time").dataType(Integer.class).make();
        management.makePropertyKey("reason").dataType(String.class).make();
        management.makePropertyKey("place").dataType(Geoshape.class).make();
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
            log.info("creating elements");

            // see GraphOfTheGodsFactory.java

            final Vertex saturn = g.addV("titan").property("name", "saturn").property("age", 10000).next();
            final Vertex sky = g.addV("location").property("name", "sky").next();
            final Vertex sea = g.addV("location").property("name", "sea").next();
            final Vertex jupiter = g.addV("god").property("name", "jupiter").property("age", 5000).next();
            final Vertex neptune = g.addV("god").property("name", "neptune").property("age", 4500).next();
            final Vertex hercules = g.addV("demigod").property("name", "hercules").property("age", 30).next();
            final Vertex alcmene = g.addV("human").property("name", "alcmene").property("age", 45).next();
            final Vertex pluto = g.addV("god").property("name", "pluto").property("age", 4000).next();
            final Vertex nemean = g.addV("monster").property("name", "nemean").next();
            final Vertex hydra = g.addV("monster").property("name", "hydra").next();
            final Vertex cerberus = g.addV("monster").property("name", "cerberus").next();
            final Vertex tartarus = g.addV("location").property("name", "tartarus").next();

            g.V(jupiter).as("a").V(saturn).addE("father").from("a").next();
            g.V(jupiter).as("a").V(sky).addE("lives").property("reason", "loves fresh breezes").from("a").next();
            g.V(jupiter).as("a").V(neptune).addE("brother").from("a").next();
            g.V(jupiter).as("a").V(pluto).addE("brother").from("a").next();

            g.V(neptune).as("a").V(sea).addE("lives").property("reason", "loves waves").from("a").next();
            g.V(neptune).as("a").V(jupiter).addE("brother").from("a").next();
            g.V(neptune).as("a").V(pluto).addE("brother").from("a").next();

            g.V(hercules).as("a").V(jupiter).addE("father").from("a").next();
            g.V(hercules).as("a").V(alcmene).addE("mother").from("a").next();


            g.V(pluto).as("a").V(jupiter).addE("brother").from("a").next();
            g.V(pluto).as("a").V(neptune).addE("brother").from("a").next();
            g.V(pluto).as("a").V(tartarus).addE("lives").property("reason", "no fear of death").from("a").next();
            g.V(pluto).as("a").V(cerberus).addE("pet").from("a").next();

            g.V(cerberus).as("a").V(tartarus).addE("lives").from("a").next();

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

            log.info("reading elements");

            // look up vertex by name can use a composite index in JanusGraph
            final Optional<Map<Object, Object>> v = g.V().has("name", "jupiter").valueMap(true).tryNext();
            if (v.isPresent()) {
                log.info(v.get().toString());
            } else {
                log.warn("jupiter not found");
            }

            // look up an incident edge
            final Optional<Map<Object, Object>> edge = g.V().has("name", "hercules").outE("battled").as("e").inV()
                .has("name", "hydra").select("e").valueMap(true).tryNext();
            if (edge.isPresent()) {
                log.info(edge.get().toString());
            } else {
                log.warn("hercules battled hydra not found");
            }

            // numerical range query can use a mixed index in JanusGraph
            final List<Object> list = g.V().has("age", P.gte(5000)).values("age").toList();
            log.info(list.toString());

            // pluto might be deleted
            final boolean plutoExists = g.V().has("name", "pluto").hasNext();
            if (plutoExists) {
                log.info("pluto exists");
            } else {
                log.warn("pluto not found");
            }

            // look up jupiter's brothers
            final List<Object> brothers = g.V().has("name", "jupiter").both("brother").values("name").dedup().toList();
            log.info("jupiter's brothers: " + brothers.toString());

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
            log.info("updating elements");
            final long ts = System.currentTimeMillis();
            g.V().has("name", "jupiter").property("ts", ts).iterate();
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
            log.info("deleting elements");
            // note that this will succeed whether or not pluto exists
            g.V().has("name", "pluto").drop().iterate();
            g.tx().commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            g.tx().rollback();
        }
    }
}
