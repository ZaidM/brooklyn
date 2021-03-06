/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package brooklyn.entity.messaging.storm;

import java.util.Map;

import org.testng.annotations.Test;

import brooklyn.util.collections.MutableMap;

@Test(groups="Live")
public class StormGceLiveTest extends StormAbstractCloudLiveTest {

    private static final String NAMED_LOCATION = "gce-europe-west1";
    private static final String LOCATION_ID = "gce-europe-west1-a";
    private static final String URI = "https://www.googleapis.com/compute/v1beta15/projects/google/global/images/centos-6-v20130325";
    private static final String IMAGE_ID = "centos-6-v20130325";

    @Override
    public String getLocation() {
        return NAMED_LOCATION;
    }

    @Override
    public Map<String, ?> getFlags() {
        return MutableMap.of(
                "locationId", LOCATION_ID,
                "imageId", IMAGE_ID,
                "uri", URI + IMAGE_ID,
                "groupId", "storm-test",
                "stopIptables", "true"
        );
    }

}
