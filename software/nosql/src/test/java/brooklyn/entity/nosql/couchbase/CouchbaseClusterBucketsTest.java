package brooklyn.entity.nosql.couchbase;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.test.Asserts;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.http.HttpTool;
import brooklyn.util.http.HttpToolResponse;
import brooklyn.util.time.Duration;


public class CouchbaseClusterBucketsTest {

    protected TestApplication app;
    protected Location testLocation;
    protected CouchbaseCluster cluster;
    private String provider = "named:couchbasetest";

    @BeforeMethod(alwaysRun = true)
    public void setup() {
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
        testLocation = app.getManagementContext().getLocationRegistry().resolve(provider);
    }

    @AfterMethod(alwaysRun = true)
    public void shutdown() {
        Entities.destroyAll(app.getManagementContext());
    }

    @Test(groups = "Live")
    public void canCreateBuckets() throws Exception {

        Map<String, Object> bucket1 = Maps.newHashMap();
        Map<String, Object> bucket2 = Maps.newHashMap();

        bucket1.put("bucket", "bucket1");
        bucket1.put("bucket-ramsize", 100);
        bucket1.put("bucket-port",11122);

        bucket2.put("bucket", "bucket2");
        bucket2.put("bucket-ramsize", 100);
        bucket2.put("bucket-port",11123);

        cluster = app.createAndManageChild(EntitySpec.create(CouchbaseCluster.class)
                .configure("initialSize", 2)
                .configure(CouchbaseCluster.CREATE_BUCKETS, ImmutableList.of(bucket1,bucket2)));

        app.start(ImmutableList.of(testLocation));

        //http://localhost:8091/pools/default/buckets
        CouchbaseNode anyNode = (CouchbaseNode) Iterables.find(cluster.getMembers(), Predicates.instanceOf(CouchbaseNode.class));

        final String hostname = anyNode.getAttribute(Attributes.HOSTNAME);
        final String webPort = anyNode.getAttribute(CouchbaseNode.COUCHBASE_WEB_ADMIN_PORT).toString();

        Map<String, ?> flags = ImmutableMap.of("timeout", Duration.ONE_MINUTE);

        Asserts.eventually(flags, new Supplier<Boolean>() {
            @Override
            public Boolean get()  {
                try {
                    URI baseUri = new URI(format("http://Administrator:password@%s:%s/pools/default/buckets", hostname, webPort));
                    HttpClient client = HttpTool.httpClientBuilder().build();
                    HttpToolResponse result = HttpTool.httpGet(client, baseUri, ImmutableMap.<String, String>of());
                    Assert.assertEquals(result.getResponseCode(), 200);

                    ByteArrayOutputStream boas = new ByteArrayOutputStream();
                    boas.write(result.getContent());

                    String response = boas.toString();

                    JSONArray jsonResult = (JSONArray) new JSONParser().parse(response);
                    List<String> serverBucketNames = Lists.newArrayList();

                    for (Iterator<Object> jsonIt = jsonResult.iterator(); jsonIt.hasNext(); ) {
                        serverBucketNames.add((String) ((JSONObject) jsonIt.next()).get("name"));
                    }

                    return serverBucketNames.contains("bucket1") && serverBucketNames.contains("bucket2");
                } catch (Exception e) {
                    return false;
                }
        }}, Predicates.equalTo(true));


        Entities.dumpInfo(app);

    }
}
