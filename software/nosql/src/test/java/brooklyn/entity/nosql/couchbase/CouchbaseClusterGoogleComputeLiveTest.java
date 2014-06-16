package brooklyn.entity.nosql.couchbase;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import brooklyn.entity.AbstractGoogleComputeLiveTest;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.test.EntityTestUtils;

public class CouchbaseClusterGoogleComputeLiveTest extends AbstractGoogleComputeLiveTest {
    @Override
    protected void doTest(Location loc) throws Exception {
        CouchbaseCluster cluster = app.createAndManageChild(EntitySpec.create(CouchbaseCluster.class)
                .configure(CouchbaseCluster.INITIAL_SIZE, 2)
                .configure(CouchbaseCluster.MEMBER_SPEC, EntitySpec.create(CouchbaseNode.class)));
        app.start(ImmutableList.of(loc));

        EntityTestUtils.assertAttributeEqualsEventually(cluster, CouchbaseCluster.SERVICE_UP, true);

        CouchbaseNode first = (CouchbaseNode) Iterables.get(cluster.getMembers(), 0);
        CouchbaseNode second = (CouchbaseNode) Iterables.get(cluster.getMembers(), 1);

        EntityTestUtils.assertAttributeEqualsEventually(first, CouchbaseNode.SERVICE_UP, true);
        EntityTestUtils.assertAttributeEqualsEventually(second, CouchbaseNode.SERVICE_UP, true);

    }

    @Test(groups = {"Live"})
    @Override
    public void test_DefaultImage() throws Exception {
        super.test_DefaultImage();
    }

    @Test(enabled = false)
    public void testDummy() {
    } // Convince testng IDE integration that this really does have test methods

}
