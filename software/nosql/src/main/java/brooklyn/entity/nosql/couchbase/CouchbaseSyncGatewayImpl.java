package brooklyn.entity.nosql.couchbase;


import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;

import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.location.access.BrooklynAccessUtils;

public class CouchbaseSyncGatewayImpl extends SoftwareProcessImpl implements CouchbaseSyncGateway {

    private static final Logger log = LoggerFactory.getLogger(CouchbaseSyncGatewayImpl.class);
    private HttpFeed httpFeed;

    @Override
    public Class getDriverInterface() {
        return CouchbaseSyncGatewayDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        connectServiceUpIsRunning();
    }

    @Override
    protected void connectServiceUpIsRunning() {


        HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(this,
                getAttribute(CouchbaseSyncGateway.ADMIN_REST_API_PORT));

        String managementUri = String.format("http://%s:%s",
                hp.getHostText(), hp.getPort());

        setAttribute(MANAGEMENT_URL, managementUri);

        httpFeed = HttpFeed.builder()
                .entity(this)
                .period(200)
                .baseUri(managementUri)
                .poll(new HttpPollConfig<Boolean>(SERVICE_UP)
                        .onSuccess(HttpValueFunctions.responseCodeEquals(200)))
                .build();

    }

    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();
        disconnectServiceUpIsRunning();
    }

    @Override
    protected void disconnectServiceUpIsRunning() {
        if (httpFeed != null) {
            httpFeed.stop();
        }
    }
}
