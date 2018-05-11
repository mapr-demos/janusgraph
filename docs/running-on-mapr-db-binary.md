# Running JanusGraph on MapR-DB Binary

MapR-DB Binary provides the same API as Apache HBase with several [limitations](). Thus, HBase applications can be 
easily migrated to run on top of MapR-DB Binary. This document explains how to build and run JanusGraph on MapR-DB.

## Changing dependencies

In order to use MapR artifacts instead of Apache ones, we need to add MapR Maven repository to 
the parent JanusGraph [pom.xml](https://github.com/mapr-demos/janusgraph/blob/master/pom.xml#L156)

```xml
    <repository>
        <id>mapr-releases</id>
        <url>http://repository.mapr.com/maven/</url>
        <snapshots><enabled>true</enabled></snapshots>
        <releases><enabled>true</enabled></releases>
    </repository>
```

Also, we need to set appropriate versions for Hadoop, Hbase and ZooKeeper artifacts:

```xml
    <properties>
        <hadoop2.version>2.7.0-mapr-1710</hadoop2.version>
        <hbase100.core.version>1.1.8-mapr-1710</hbase100.core.version>
        <hbase098.core.version>0.98.12-mapr-1602-m7-5.1.0</hbase098.core.version>
    </properties>
```

We change the scope of these artifacts to be `provided` and use Jars, which are present on MapR node, using 
`hbase classpath`, `hadoop classpath` and `mapr classpath` commands. 

## Changing classpath

In order to use MapR artifacts from the cluster node, we have to append jars to the classpath. Modify 
[gremlin.sh](https://github.com/mapr-demos/janusgraph/blob/master/janusgraph-dist/src/assembly/static/bin/gremlin.sh#L83) 
and 
[gremlin-server.sh](https://github.com/mapr-demos/janusgraph/blob/master/janusgraph-dist/src/assembly/static/bin/gremlin-server.sh#L37) 
scripts to append MapR artifacts to the classpath:

```
...

export CLASSPATH="${CLASSPATH:-}:$CP:`hbase classpath`:`hadoop classpath`:`mapr classpath`"

...

```

## Build distribution

Run the following command in order to build MapR version of distribution archive:
```
mvn clean install -Pjanusgraph-mapr-release -Dgpg.skip=true -DskipTests=true
```
This command generates the distribution archive in 
`janusgraph-dist/janusgraph-dist-hadoop-2/target/janusgraph-$VERSION-hadoop2.zip`. 

## Run basic examples

* Copy newly created archive on one of the MapR Cluster's nodes. 
* Unzip it `janusgraph-$VERSION-hadoop2.zip`. 
* Add following properties to `conf/janusgraph-hbase.properties`:
```
storage.hostname=maprnodename
storage.port=5181
storage.hbase.ext.hbase.zookeeper.property.clientPort=5181
storage.hbase.table=/janusgraph
```

* Run `gremlin.sh`

```
./bin/gremlin.sh
```

* Open the graph according to `conf/janusgraph-hbase.properties` file:

```
gremlin> graph = JanusGraphFactory.open('conf/janusgraph-hbase.properties')

```

* Load test graph(described [here](http://docs.janusgraph.org/latest/getting-started.html)):

```
gremlin> GraphOfTheGodsFactory.loadWithoutMixedIndex(graph, true)

```

* Retrieve the Saturn vertex:

```
gremlin> g = graph.traversal()

gremlin> saturn = g.V().has('name', 'saturn').next()
==>v[256]
gremlin> g.V(saturn).valueMap()
==>[name:[saturn], age:[10000]]
```