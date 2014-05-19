package brooklyn.demo;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.StartableApplication;
import brooklyn.entity.nosql.couchbase.CouchbaseCluster;
import brooklyn.entity.nosql.couchbase.CouchbaseNode;
import brooklyn.entity.nosql.couchbase.CouchbaseSyncGateway;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.launcher.BrooklynLauncher;
import brooklyn.policy.PolicySpec;
import brooklyn.policy.ha.ServiceFailureDetector;
import brooklyn.policy.ha.ServiceReplacer;
import brooklyn.policy.ha.ServiceRestarter;
import brooklyn.util.CommandLineUtil;

public class CouchbaseClusterExample extends AbstractApplication {

    public static void main(String[] argv) {
        List<String> args = Lists.newArrayList(argv);
        String port = CommandLineUtil.getCommandLineOption(args, "--port", "8081+");
        String location = CommandLineUtil.getCommandLineOption(args, "--location", "named:couchbasetest");
        Preconditions.checkArgument(args.isEmpty(), "Unsupported args: " + args);

        BrooklynLauncher launcher = BrooklynLauncher.newInstance()
                .application(EntitySpec.create(StartableApplication.class, CouchbaseClusterExample.class))
                .webconsolePort(port)
                .location(location)
                .start();

        Entities.dumpInfo(launcher.getApplications());
    }

    public void init() {

        Map<String, Object> bucket = Maps.newHashMap();
        bucket.put("bucket", "sync_gateway");

        CouchbaseCluster cbCluster = addChild(EntitySpec.create(CouchbaseCluster.class)
                .configure(CouchbaseCluster.INITIAL_SIZE, 2)
                .configure(CouchbaseCluster.CREATE_BUCKETS, ImmutableList.of(bucket))
                .configure(CouchbaseCluster.MEMBER_SPEC, EntitySpec.create(CouchbaseNode.class)
                        .policy(PolicySpec.create(ServiceFailureDetector.class))
                        .policy(PolicySpec.create(ServiceRestarter.class)
                                .configure(ServiceRestarter.FAILURE_SENSOR_TO_MONITOR, ServiceFailureDetector.ENTITY_FAILED)))
                .policy(PolicySpec.create(ServiceReplacer.class)
                        .configure(ServiceReplacer.FAILURE_SENSOR_TO_MONITOR, ServiceRestarter.ENTITY_RESTART_FAILED)));

        addChild(EntitySpec.create(CouchbaseSyncGateway.class)
                .configure(CouchbaseSyncGateway.COUCHBASE_SERVER, cbCluster));
    }
}
