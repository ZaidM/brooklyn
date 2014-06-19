package brooklyn.entity.nosql.couchbase;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.beust.jcommander.internal.Maps;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import brooklyn.entity.AbstractEc2LiveTest;
import brooklyn.entity.nosql.riak.RiakNodeEc2LiveTest;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.test.EntityTestUtils;

public class CouchbaseEc2LiveTest extends AbstractEc2LiveTest {

    private static final Logger log = LoggerFactory.getLogger(RiakNodeEc2LiveTest.class);


    @Override
    protected void doTest(Location loc) throws Exception {

        Map<String, Object> bucket1 = Maps.newHashMap();
        bucket1.put("bucket", "test_bucket");
        bucket1.put("bucket-type", "couchbase");
        bucket1.put("bucket-port", 11222);
        bucket1.put("bucket-ramsize", 100);
        bucket1.put("bucket-replica", 1);

        Map<String, Object> bucket2 = Maps.newHashMap();
        bucket1.put("bucket", "test_bucket2");
        bucket1.put("bucket-type", "couchbase");
        bucket1.put("bucket-port", 11222);
        bucket1.put("bucket-ramsize", 100);
        bucket1.put("bucket-replica", 1);

        CouchbaseCluster cluster = app.createAndManageChild(EntitySpec.create(CouchbaseCluster.class)
                .configure(CouchbaseCluster.INITIAL_SIZE, 2)
                .configure(CouchbaseCluster.CREATE_BUCKETS, ImmutableList.of(bucket1, bucket2))
                .configure(CouchbaseCluster.MEMBER_SPEC, EntitySpec.create(CouchbaseNode.class)));
        app.start(ImmutableList.of(loc));

        EntityTestUtils.assertAttributeEqualsEventually(cluster, CouchbaseNode.SERVICE_UP, true);

        CouchbaseNode first = (CouchbaseNode) Iterables.get(cluster.getMembers(), 0);
        CouchbaseNode second = (CouchbaseNode) Iterables.get(cluster.getMembers(), 1);

        EntityTestUtils.assertAttributeEqualsEventually(first, CouchbaseNode.SERVICE_UP, true);
        EntityTestUtils.assertAttributeEqualsEventually(second, CouchbaseNode.SERVICE_UP, true);

        EntityTestUtils.assertAttributeEqualsEventually(first, CouchbaseNode.IS_IN_CLUSTER, true);
        EntityTestUtils.assertAttributeEqualsEventually(second, CouchbaseNode.IS_IN_CLUSTER, true);
    }

    @Test(enabled = false)
    public void testDummy() {
    } // Convince TestNG IDE integration that this really does have test methods

    @Override
    public void test_Ubuntu_12_0() throws Exception {
        super.test_Ubuntu_12_0();
    }

}
