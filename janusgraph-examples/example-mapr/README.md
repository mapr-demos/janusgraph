# MapR-DB Binary Storage Example

## About MapR-DB Binary

[Apache HBase](http://hbase.apache.org/) is a scalable, distributed big data
store.

### MapR-DB Binary configuration

The JanusGraph `conf/mapr-db-example.properties` properties file assumes that HBase is installed on localhost
using its quickstart configuration. It uses MapR ZooKeeper on `5181` port and ready to be used on one of the MapR 
Cluster nodes.

## Dependencies

The required Maven dependencies for MapR-DB Binary the same as for HBase:

```
        <dependency>
            <groupId>org.janusgraph</groupId>
            <artifactId>janusgraph-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.janusgraph</groupId>
            <artifactId>janusgraph-hbase</artifactId>
            <version>${project.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-classic</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
```

## Run the example


Note, that JanusGraph parent `pom.xml` declares all Hadoop and HBase dependencies with scope `provided`. Thus, we 
must add to classpath actual artifacts from MapR Cluster's Node.

This command can be run from the `examples` or the project's directory.

*) Build the example app from `examples/example-mapr` directory:

```
mvn clean package
```

*) Copy built jar and `conf/mapr-db-example.properties` properties file on one of the MapR cluster nodes:

```
$ export NODE_HOSTNAME=yournodename
$ scp target/example-mapr.jar mapr@${NODE_HOSTNAME}:/home/mapr
$ scp conf/mapr-db-example.properties mapr@${NODE_HOSTNAME}:/home/mapr
```

*) Run the app on MapR cluster node:

```
$ ssh mapr@${NODE_HOSTNAME}
$ java -cp example-mapr.jar:`hbase classpath`:`hadoop classpath`:`mapr classpath` org.janusgraph.example.JanusGraphMapRApp mapr-db-example.properties
```

Note, that in command above we append HBase and Hadoop artifacts to the classpath.

## Drop the graph

After running an example, you may want to drop the graph from storage. Make
sure to stop the application before dropping the graph. Run this command on MapR Cluster's Node:

```
$ java -cp example-mapr.jar:`hbase classpath`:`hadoop classpath`:`mapr classpath` org.janusgraph.example.JanusGraphMapRApp drop
```
