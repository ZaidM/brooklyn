package brooklyn.entity.nosql.couchbase;

import static brooklyn.util.JavaGroovyEquivalents.groovyTruth;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityInternal;
import brooklyn.entity.basic.Lifecycle;
import brooklyn.entity.group.AbstractMembershipTrackingPolicy;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.event.basic.DependentConfiguration;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.location.Location;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.collections.MutableSet;
import brooklyn.util.task.DynamicTasks;
import brooklyn.util.task.TaskBuilder;
import brooklyn.util.time.Time;

public class CouchbaseClusterImpl extends DynamicClusterImpl implements CouchbaseCluster {
    private static final Logger log = LoggerFactory.getLogger(CouchbaseClusterImpl.class);
    private final Object mutex = new Object[0];
    private final HttpFeed[] resetBucketCreation = new HttpFeed[]{null};
    private volatile ExecutorService bucketCreationExecutor;

    public void init() {
        super.init();
        log.info("Initializing the Couchbase cluster...");
//        resetBucketCreation = <new http feed, polling to see if buckets are being created... defaults to STOPPED>
        setAttribute(BUCKET_CREATION_IN_PROGRESS, false);
        bucketCreationExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);

        connectSensors();
        connectEnrichers();

        //start timeout before adding the servers
        Time.sleep(getConfig(SERVICE_UP_TIME_OUT));

        Optional<Set<Entity>> upNodes = Optional.<Set<Entity>>fromNullable(getAttribute(COUCHBASE_CLUSTER_UP_NODES));
        if (upNodes.isPresent() && !upNodes.get().isEmpty()) {

            //TODO: select a new primary node if this one fails
            Entity primaryNode = upNodes.get().iterator().next();
            ((EntityInternal) primaryNode).setAttribute(CouchbaseNode.IS_PRIMARY_NODE, true);
            setAttribute(COUCHBASE_PRIMARY_NODE, primaryNode);

            Set<Entity> serversToAdd = MutableSet.<Entity>copyOf(getUpNodes());
            serversToAdd.remove(getPrimaryNode());

            if (getUpNodes().size() >= getQuorumSize() && getUpNodes().size() > 0) {
                log.info("number of SERVICE_UP nodes:{} in cluster:{} did reached Quorum:{}, adding the servers", new Object[]{getUpNodes().size(), getId(), getQuorumSize()});
                addServers(serversToAdd);

                //wait for servers to be added to the couchbase server
                Time.sleep(getConfig(DELAY_BEFORE_ADVERTISING_CLUSTER));
                Entities.invokeEffector(this, getPrimaryNode(), CouchbaseNode.REBALANCE);

                setAttribute(IS_CLUSTER_INITIALIZED, true);

                //wait for rebalance then add buckets

                if (Optional.fromNullable(CREATE_BUCKETS).isPresent()) {
                    createBuckets();
                }
            } else {
                //TODO: add a repeater to wait for a quorum of servers to be up.
                //retry waiting for service up?
                //check Repeater.
            }
        } else {
            setAttribute(SERVICE_STATE, Lifecycle.ON_FIRE);
        }


    }

    @Override
    public void stop() {
        super.stop();
    }

    public void connectEnrichers() {

        subscribeToMembers(this, SERVICE_UP, new SensorEventListener<Boolean>() {
            @Override
            public void onEvent(SensorEvent<Boolean> event) {
                setAttribute(SERVICE_UP, calculateServiceUp());
            }
        });
    }

    protected void connectSensors() {
        Map<String, Object> flags = MutableMap.<String, Object>builder()
                .put("name", "Controller targets tracker")
                .build();

        AbstractMembershipTrackingPolicy serverPoolMemberTrackerPolicy = new AbstractMembershipTrackingPolicy(flags) {
            protected void onEntityChange(Entity member) {
                onServerPoolMemberChanged(member);
            }

            protected void onEntityAdded(Entity member) {
                onServerPoolMemberChanged(member);
            }

            protected void onEntityRemoved(Entity member) {
                onServerPoolMemberChanged(member);
            }
        };

        addPolicy(serverPoolMemberTrackerPolicy);
        serverPoolMemberTrackerPolicy.setGroup(this);


    }

    protected synchronized void onServerPoolMemberChanged(Entity member) {
        if (log.isTraceEnabled()) log.trace("For {}, considering membership of {} which is in locations {}",
                new Object[]{this, member, member.getLocations()});

        //FIXME: make use of servers to be added after cluster initialization.
        synchronized (mutex) {
            if (belongsInServerPool(member)) {

                Optional<Set<Entity>> upNodes = Optional.fromNullable(getUpNodes());
                if (upNodes.isPresent()) {

                    if (!upNodes.get().contains(member)) {
                        Set<Entity> newNodes = Sets.newHashSet(getUpNodes());
                        newNodes.add(member);
                        setAttribute(COUCHBASE_CLUSTER_UP_NODES, newNodes);

                        //add to set of servers to be added.
                        if (isClusterInitialized()) {
                            addServer(member);
                        }
                    } else {
                        log.warn("Node already in cluster up nodes {}: {};", this, member);
                    }
                } else {
                    Set<Entity> newNodes = Sets.newHashSet();
                    newNodes.add(member);
                    setAttribute(COUCHBASE_CLUSTER_UP_NODES, newNodes);

                    if (isClusterInitialized()) {
                        addServer(member);
                    }
                }
            } else {
                Set<Entity> upNodes = getUpNodes();
                if (upNodes != null && upNodes.contains(member)) {
                    upNodes.remove(member);
                    setAttribute(COUCHBASE_CLUSTER_UP_NODES, upNodes);
                    log.info("Removing couchbase node {}: {}; from cluster", new Object[]{this, member});
                }
            }
            if (log.isTraceEnabled()) log.trace("Done {} checkEntity {}", this, member);
        }
    }

    protected boolean belongsInServerPool(Entity member) {
        if (!groovyTruth(member.getAttribute(Startable.SERVICE_UP))) {
            if (log.isTraceEnabled()) log.trace("Members of {}, checking {}, eliminating because not up", this, member);
            return false;
        }
        if (!getMembers().contains(member)) {
            if (log.isTraceEnabled())
                log.trace("Members of {}, checking {}, eliminating because not member", this, member);

            return false;
        }
        if (log.isTraceEnabled()) log.trace("Members of {}, checking {}, approving", this, member);

        return true;
    }


    protected EntitySpec<?> getMemberSpec() {
        EntitySpec<?> result = super.getMemberSpec();
        if (result != null) return result;
        return EntitySpec.create(CouchbaseNode.class);
    }


    protected int getQuorumSize() {
        Integer quorumSize = getConfig(CouchbaseCluster.INITIAL_QUORUM_SIZE);
        if (quorumSize != null && quorumSize > 0)
            return quorumSize;
        // by default the quorum would be floor(initial_cluster_size/2) + 1
        return (int) Math.floor(getConfig(INITIAL_SIZE) / 2) + 1;

    }

    protected int getActualSize() {
        return Optional.fromNullable(getAttribute(CouchbaseCluster.ACTUAL_CLUSTER_SIZE)).or(-1);
    }

    private Set<Entity> getUpNodes() {
        return getAttribute(COUCHBASE_CLUSTER_UP_NODES);
    }

    private Entity getPrimaryNode() {
        return getAttribute(COUCHBASE_PRIMARY_NODE);
    }

    @Override
    protected boolean calculateServiceUp() {
        if (!super.calculateServiceUp()) return false;
        Set<Entity> upNodes = getAttribute(COUCHBASE_CLUSTER_UP_NODES);
        if (upNodes == null || upNodes.isEmpty() || upNodes.size() < getQuorumSize()) return false;
        return true;
    }

    protected void addServers(Set<Entity> serversToAdd) {
        //FIXME: disambiguate between method names to differentiate between the stage phase and commit phase.
        log.info("adding the SERVICE_UP couchbase nodes to the cluster..");

        if (!serversToAdd.isEmpty()) {
            for (Entity e : serversToAdd) {
                if (!isMemberInCluster(e)) {
                    String hostname = e.getAttribute(Attributes.HOSTNAME) + ":" + e.getConfig(CouchbaseNode.COUCHBASE_WEB_ADMIN_PORT).iterator().next();
                    String username = e.getConfig(CouchbaseNode.COUCHBASE_ADMIN_USERNAME);
                    String password = e.getConfig(CouchbaseNode.COUCHBASE_ADMIN_PASSWORD);

                    Entities.invokeEffectorWithArgs(this, getPrimaryNode(), CouchbaseNode.SERVER_ADD, hostname, username, password);
                    //FIXME check feedback of whether the server was added.
                    ((EntityInternal) e).setAttribute(CouchbaseNode.IS_IN_CLUSTER, true);
                }
            }
        } else {
            log.warn("no servers to be added on the cluster: {}", this);
        }
    }

    protected void addServer(Entity serverToAdd) {

        if (!isMemberInCluster(serverToAdd)) {
            String hostname = serverToAdd.getAttribute(Attributes.HOSTNAME) + ":" + serverToAdd.getConfig(CouchbaseNode.COUCHBASE_WEB_ADMIN_PORT).iterator().next();
            String username = serverToAdd.getConfig(CouchbaseNode.COUCHBASE_ADMIN_USERNAME);
            String password = serverToAdd.getConfig(CouchbaseNode.COUCHBASE_ADMIN_PASSWORD);

            Entities.invokeEffectorWithArgs(this, getPrimaryNode(), CouchbaseNode.SERVER_ADD, hostname, username, password);
            //FIXME check feedback of whether the server was added.
            ((EntityInternal) serverToAdd).setAttribute(CouchbaseNode.IS_IN_CLUSTER, true);
        }
    }

    public boolean isClusterInitialized() {
        return Optional.fromNullable(getAttribute(IS_CLUSTER_INITIALIZED)).or(false);
    }

    public boolean isMemberInCluster(Entity e) {
        return Optional.fromNullable(e.getAttribute(CouchbaseNode.IS_IN_CLUSTER)).or(false);
    }

    public void createBuckets() {
        //FIXME: multiple buckets require synchronization/wait time (checks for port conflicts and exceeding ram size)
        //TODO: check for multiple bucket conflicts with port
        List<Map<String, Object>> bucketsToCreate = getConfig(CREATE_BUCKETS);
        Entity primaryNode = getPrimaryNode();

        for (Map<String, Object> bucketMap : bucketsToCreate) {
            String bucketName = bucketMap.containsKey("bucket") ? (String) bucketMap.get("bucket") : "default";
            String bucketType = bucketMap.containsKey("bucket-type") ? (String) bucketMap.get("bucket-type") : "couchbase";
            Integer bucketPort = bucketMap.containsKey("bucket-port") ? (Integer) bucketMap.get("bucket-port") : 11222;
            Integer bucketRamSize = bucketMap.containsKey("bucket-ramsize") ? (Integer) bucketMap.get("bucket-ramsize") : 200;
            Integer bucketReplica = bucketMap.containsKey("bucket-replica") ? (Integer) bucketMap.get("bucket-replica") : 1;

            log.info("adding bucket: {} to primary node: {}", bucketName, primaryNode.getId());
            createBucket(primaryNode, bucketName, bucketType, bucketPort, bucketRamSize, bucketReplica);
        }
    }

    public void createBucket(final Entity primaryNode, final String bucketName, final String bucketType, final Integer bucketPort, final Integer bucketRamSize, final Integer bucketReplica) {
        DynamicTasks.queueIfPossible(TaskBuilder.<Void>builder().name("Creating bucket " + bucketName).body(
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        DependentConfiguration.waitInTaskForAttributeReady(CouchbaseClusterImpl.this, CouchbaseCluster.BUCKET_CREATION_IN_PROGRESS, Predicates.equalTo(false));
                        if (CouchbaseClusterImpl.this.resetBucketCreation[0] != null) {
                            CouchbaseClusterImpl.this.resetBucketCreation[0].stop();
                        }
                        setAttribute(CouchbaseCluster.BUCKET_CREATION_IN_PROGRESS, true);

                        CouchbaseClusterImpl.this.resetBucketCreation[0] = HttpFeed.builder()
                                .entity(CouchbaseClusterImpl.this)
                                .period(500, TimeUnit.MILLISECONDS)
                                .baseUri(String.format("%s/pools/default/buckets/%s", primaryNode.getAttribute(CouchbaseNode.COUCHBASE_WEB_ADMIN_URL), bucketName))
                                .credentials(primaryNode.getConfig(CouchbaseNode.COUCHBASE_ADMIN_USERNAME), primaryNode.getConfig(CouchbaseNode.COUCHBASE_ADMIN_PASSWORD))
                                .poll(new HttpPollConfig<Boolean>(BUCKET_CREATION_IN_PROGRESS)
                                        .onSuccess(HttpValueFunctions.responseCodeEquals(404))
                                        .onFailureOrException(Functions.constant(false)))
                                .build();

                        Entities.invokeEffectorWithArgs(CouchbaseClusterImpl.this, primaryNode, CouchbaseNode.BUCKET_CREATE, bucketName, bucketType, bucketPort, bucketRamSize, bucketReplica);
                        DependentConfiguration.waitInTaskForAttributeReady(CouchbaseClusterImpl.this, CouchbaseCluster.BUCKET_CREATION_IN_PROGRESS, Predicates.equalTo(false));
                        if (CouchbaseClusterImpl.this.resetBucketCreation[0] != null) {
                            CouchbaseClusterImpl.this.resetBucketCreation[0].stop();
                        }
                        return null;
                    }
                }
        ).build()).orSubmitAndBlock();
    }

    @Override
    public void bucketCreate(String bucketName, String bucketType, Integer bucketPort, Integer bucketRamSize, Integer bucketReplica) {
        createBucket(getPrimaryNode(), bucketName, bucketType, bucketPort, bucketRamSize, bucketReplica);
    }
}
