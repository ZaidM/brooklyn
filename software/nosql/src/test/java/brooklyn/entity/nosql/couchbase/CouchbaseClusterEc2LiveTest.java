package brooklyn.entity.nosql.couchbase;

import static java.lang.String.format;

import java.net.URI;
import java.util.ArrayList;

import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import brooklyn.entity.AbstractEc2LiveTest;
import brooklyn.entity.Entity;
import brooklyn.entity.nosql.mongodb.MongoDBEc2LiveTest;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.test.EntityTestUtils;
import brooklyn.util.http.HttpTool;
import brooklyn.util.http.HttpToolResponse;

public class CouchbaseClusterEc2LiveTest extends AbstractEc2LiveTest {

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBEc2LiveTest.class);

    @Override
    protected void doTest(Location loc) throws Exception {
        CouchbaseCluster cluster = app.createAndManageChild(EntitySpec.create(CouchbaseCluster.class)
                .configure("initialSize", 2));

        app.start(ImmutableList.of(loc));

        EntityTestUtils.assertAttributeEqualsEventually(cluster, CouchbaseCluster.SERVICE_UP, true);
        EntityTestUtils.assertAttributeEqualsEventually(cluster, CouchbaseCluster.GROUP_SIZE, 2);

        ArrayList<Entity> cbNodes = (ArrayList<Entity>) Iterables.filter(cluster.getMembers(), Predicates.instanceOf(CouchbaseNode.class));

        EntityTestUtils.assertAttributeEqualsEventually(cbNodes.get(0), CouchbaseNode.SERVICE_UP, true);
        EntityTestUtils.assertAttributeEqualsEventually(cbNodes.get(1), CouchbaseNode.SERVICE_UP, true);

        //check
        String webAdminUrl = cbNodes.get(0).getAttribute(CouchbaseNode.COUCHBASE_WEB_ADMIN_URL);
        URI baseUri = new URI(format("%s/pools", webAdminUrl));
        HttpClient client = HttpTool.httpClientBuilder().build();
        HttpToolResponse result = HttpTool.httpGet(client, baseUri, ImmutableMap.<String, String>of());
        Assert.assertEquals(result.getResponseCode(), 200);
    }

    @Test(enabled = false)
    public void testDummy() {
    } // Convince TestNG IDE integration that this really does have test methods
}
