package brooklyn.entity.nosql.couchbase;


import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.util.time.Duration;

public class CouchbaseSyncGatewayImpl extends SoftwareProcessImpl implements CouchbaseSyncGateway {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseSyncGatewayImpl.class);

    @Override
    public Class getDriverInterface() {
        return CouchbaseSyncGatewayDriver.class;
    }

    public Entity getCouchbaseNode() {
        return getConfig(CouchbaseSyncGateway.COUCHBASE_NODE);
    }
}
