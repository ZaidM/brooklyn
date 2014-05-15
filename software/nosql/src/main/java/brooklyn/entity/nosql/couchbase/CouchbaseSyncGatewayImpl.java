package brooklyn.entity.nosql.couchbase;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.SoftwareProcessImpl;

public class CouchbaseSyncGatewayImpl extends SoftwareProcessImpl implements CouchbaseSyncGateway {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseSyncGatewayImpl.class);

    @Override
    public Class getDriverInterface() {
        return CouchbaseSyncGatewayDriver.class;
    }

}
