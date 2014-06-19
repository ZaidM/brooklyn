package brooklyn.demo;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import brooklyn.catalog.Catalog;
import brooklyn.catalog.CatalogConfig;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.nosql.couchbase.CouchbaseCluster;
import brooklyn.entity.nosql.couchbase.CouchbaseNode;
import brooklyn.entity.nosql.couchbase.CouchbaseSyncGateway;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.policy.PolicySpec;
import brooklyn.policy.ha.ServiceFailureDetector;
import brooklyn.policy.ha.ServiceRestarter;
import brooklyn.util.CommandLineUtil;

@Catalog(name = "Couchbase Cluster Example", description = "Couchbase Cluster deployment blueprint")
public class CouchbaseClusterExample extends AbstractApplication {


    public static final String DEFAULT_LOCATION_SPEC = "aws-ec2:us-east-1";

    @CatalogConfig(label = "Couchbase Cluster Size")
    public static final ConfigKey<Integer> COUCHBASE_CLUSTER_SIZE = ConfigKeys.newConfigKey(
            "couchbase.cluster.size", "Initial size of the Couchbase Cluster", 2);

    public static void main(String[] argv) {
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", DEFAULT_LOCATION_SPEC);
        Preconditions.checkArgument(args.isEmpty(), "Unsupported args: " + args);

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpec.create(StartableApplication.class, CouchbaseClusterExample.class))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }

    public void init() {
        Map<String, Object> bucket1 = Maps.newHashMap();
        bucket1.put("bucket", "test_bucket");
        bucket1.put("bucket-type", "couchbase");
        bucket1.put("bucket-port", 11222);
        bucket1.put("bucket-ramsize", 200);
        bucket1.put("bucket-replica", 1);

        CouchbaseCluster cluster = addChild(EntitySpec.create(CouchbaseCluster.class)
                .configure(CouchbaseCluster.INITIAL_SIZE, getConfig(COUCHBASE_CLUSTER_SIZE))
                .configure(CouchbaseCluster.CREATE_BUCKETS, ImmutableList.of(bucket1))
                .configure(CouchbaseCluster.MEMBER_SPEC, EntitySpec.create(CouchbaseNode.class)
                        .policy(PolicySpec.create(ServiceFailureDetector.class))
                        .policy(PolicySpec.create(ServiceRestarter.class)
                                .configure(ServiceRestarter.FAILURE_SENSOR_TO_MONITOR, ServiceFailureDetector.ENTITY_FAILED))));

        addChild(EntitySpec.create(CouchbaseSyncGateway.class)
                .configure(CouchbaseSyncGateway.COUCHBASE_SERVER, cluster)
                .configure(CouchbaseSyncGateway.COUCHBASE_SERVER_BUCKET, "test_bucket"));
    }
}


