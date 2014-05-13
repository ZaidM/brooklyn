package brooklyn.entity.nosql.couchbase;

import brooklyn.entity.basic.SoftwareProcessImpl;

public class CouchbaseSyncGatewayImpl extends SoftwareProcessImpl implements CouchbaseSyncGateway {
    @Override
    public Class getDriverInterface() {
        return CouchbaseSyncGatewayDriver.class;
    }
}
