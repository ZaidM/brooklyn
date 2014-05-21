package brooklyn.entity.nosql.couchbase;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.reflect.TypeToken;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.time.Duration;

@ImplementedBy(CouchbaseClusterImpl.class)
public interface CouchbaseCluster extends DynamicCluster {

    AttributeSensor<Integer> ACTUAL_CLUSTER_SIZE = Sensors.newIntegerSensor("coucbase.cluster.actualClusterSize", "returns the actual number of nodes in the cluster");

    @SuppressWarnings("serial")
    AttributeSensor<Set<Entity>> COUCHBASE_CLUSTER_UP_NODES = Sensors.newSensor(new TypeToken<Set<Entity>>() {
    }, "couchbase.cluster.clusterEntities", "the set of service up nodes");

    @SuppressWarnings("serial")
    AttributeSensor<List<String>> COUCHBASE_CLUSTER_BUCKETS = Sensors.newSensor(new TypeToken<List<String>>() {
    }, "couchbase.cluster.buckets", "Names of all the buckets the couchbase cluster");

    AttributeSensor<Entity> COUCHBASE_PRIMARY_NODE = Sensors.newSensor(Entity.class, "couchbase.cluster.primaryNode", "The primary couchbase node to query and issue add-server and rebalance on");

    AttributeSensor<Boolean> IS_CLUSTER_INITIALIZED = Sensors.newBooleanSensor("couchbase.cluster.isClusterInitialized", "flag to emit if the couchbase cluster was intialized");

    AttributeSensor<Boolean> BUCKET_CREATION_IN_PROGRESS = Sensors.newBooleanSensor("couchbase.cluster.bucketCreationInProgress", "Indicates that a bucket is currently being created, and" +
            "further bucket creation should be deferred");

    AttributeSensor<Boolean> BUCKET_CREATED = Sensors.newBooleanSensor("couchbase.cluster.bucketCreated", "Indicates that a bucket has been created");

    @SetFromFlag("intialQuorumSize")
    ConfigKey<Integer> INITIAL_QUORUM_SIZE = ConfigKeys.newIntegerConfigKey("couchbase.cluster.intialQuorumSize", "Initial cluster quorum size - number of initial nodes that must have been successfully started to report success (if < 0, then use value of INITIAL_SIZE)",
            -1);

    @SetFromFlag("delayBeforeAdvertisingCluster")
    ConfigKey<Duration> DELAY_BEFORE_ADVERTISING_CLUSTER = ConfigKeys.newConfigKey(Duration.class, "couchbase.cluster.delayBeforeAdvertisingCluster", "Delay after cluster is started before checking and advertising its availability", Duration.THIRTY_SECONDS);

    @SetFromFlag("serviceUpTimeOut")
    ConfigKey<Duration> SERVICE_UP_TIME_OUT = ConfigKeys.newConfigKey(Duration.class, "couchbase.cluster.serviceUpTimeOut", "Service up time out duration for all the couchbase nodes", Duration.seconds(3 * 60));

    /**
     * createBuckets is a list of all the buckets to be created on the couchbase cluster
     * the buckets will be created on the primary node of the cluster
     * each map entry for a bucket should contain the following parameters:
     * - <"bucket",(String) name of the bucket (default: default)>
     * - <"bucket-type",(String) name of bucket type (default: couchbase)>
     * - <"bucket-port",(Integer) the bucket port to connect to (default: 11222)>
     * - <"bucket-ramsize",(Integer) ram size allowed for bucket (default: 200)>
     * - <"bucket-replica",(Integer) number of replicas for the bucket (default: 1)>
     */
    @SetFromFlag("createBuckets")
    ConfigKey<List<Map<String, Object>>> CREATE_BUCKETS = ConfigKeys.newConfigKey(new TypeToken<List<Map<String, Object>>>() {
    },
            "couchbase.cluster.createBuckets", "a list of all dedicated port buckets to be created on the couchbase cluster");


    @Effector(description = "create a new bucket")
    public void bucketCreate(@EffectorParam(name = "bucket") String bucketName, @EffectorParam(name = "bucket-type") String bucketType,
                             @EffectorParam(name = "bucket-port") Integer bucketPort, @EffectorParam(name = "bucket-ramsize") Integer bucketRamSize,
                             @EffectorParam(name = "bucket-replica") Integer bucketReplica);

}
