package brooklyn.entity.nosql.couchbase;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;

@ImplementedBy(CouchbaseSyncGatewayImpl.class)
public interface CouchbaseSyncGateway extends SoftwareProcess {

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION,
            "1.0-beta3.1");

    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "http://packages.couchbase.com/releases/couchbase-sync-gateway/1.0-beta/couchbase-sync-gateway-community_${version}_${driver.osTag}");

    @SetFromFlag("couchbaseNode")
    ConfigKey<Entity> COUCHBASE_NODE = ConfigKeys.newConfigKey(Entity.class, "couchbaseSyncGateway.couchbaseNode", "Couchbase server node the sync gateway connects to");

    @SetFromFlag("couchbaseCluster")
    ConfigKey<CouchbaseCluster> COUCHBASE_CLUSTER = ConfigKeys.newConfigKey(CouchbaseCluster.class, "couchbaseSyncGateway.couchbaseCluster", "Couchbase cluster the sync gateway connects to");

    @SetFromFlag("serverPool")
    ConfigKey<String> COUCHBASE_SERVER_POOL = ConfigKeys.newStringConfigKey("couchbaseSyncGateway.serverPool", "Couchbase Server pool name in which to find buckets", "default");
    @SetFromFlag("couchbaseServerBucket")
    ConfigKey<String> COUCHBASE_SERVER_BUCKET = ConfigKeys.newStringConfigKey("couchbaseSyncGateway.serverBucket", "Name of the Couchbase bucket to use", "sync_gateway");

    @SetFromFlag("serverWebUrl")
    ConfigKey<String> COUCHBASE_SERVER_URL = ConfigKeys.newStringConfigKey("couchbaseSyncGateway.serverUrl", "Couchbase Server Url");

    AttributeSensor<String> COUCHBASE_SERVER_WEB_URL = Sensors.newStringSensor("couchbaseSyncGateway.serverWebUrl", "The Url and web port of the couchbase server to connect to");

//    @SetFromFlag("couchbaseNode")
//    AttributeSensor<Entity> COUCHBASE_NODE = new BasicAttributeSensorAndConfigKey<Entity>(Entity.class, "couchbaseSyncGateway.couchbaseNode", "The node in the cluster to connect to ");

    PortAttributeSensorAndConfigKey SYNC_REST_API_PORT = new PortAttributeSensorAndConfigKey("couchbaseSyncGateway.syncRestPort", "Port the Sync REST API listens on", "4984");
    PortAttributeSensorAndConfigKey ADMIN_REST_API_PORT = new PortAttributeSensorAndConfigKey("couchbaseSyncGateway.admingRestPort", "Port the Admin REST API listens on", "4985");

}
