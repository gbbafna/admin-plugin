package org.opensearch.path.to.plugin;

import org.opensearch.action.ActionResponse;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.opensearch.cluster.health.ClusterIndexHealth;
import org.opensearch.cluster.health.ClusterStateHealth;
import org.opensearch.common.ParseField;
import org.opensearch.common.Strings;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.ConstructingObjectParser;
import org.opensearch.common.xcontent.ObjectParser;
import org.opensearch.common.xcontent.StatusToXContentObject;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentParser;
import org.opensearch.rest.RestStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyMap;
import static org.opensearch.common.xcontent.ConstructingObjectParser.constructorArg;
import static org.opensearch.common.xcontent.ConstructingObjectParser.optionalConstructorArg;

/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

/**
 * Transport response for Cluster Health
 *
 */
public class ClusterHAHealthResponse extends ActionResponse implements StatusToXContentObject {
    ClusterHealthResponse clusterHealthResponse;
    ZoneAgg zoneAgg;


    public ClusterHAHealthResponse(ClusterHealthResponse clusterHealthResponse) {
        this.clusterHealthResponse = clusterHealthResponse;
    }

    public ClusterHAHealthResponse(StreamInput in) throws IOException {
        super(in);
        clusterHealthResponse = ClusterHealthResponse.readResponseFrom(in);
        zoneAgg = ZoneAgg.readResponseFrom(in);
    }


    @Override
    public void writeTo(StreamOutput out) throws IOException {
        clusterHealthResponse.writeTo(out);
        zoneAgg.writeTo(out);
    }

    @Override
    public String toString() {
        return Strings.toString(this);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("cluster_name", clusterHealthResponse.getClusterName());

        builder.startObject("active-zone");
        builder.field("nodes", this.zoneAgg.numNodes);
        builder.field("active_shards", this.zoneAgg.activeShards);
        builder.field("initializing_shards", this.zoneAgg.initializingShards);
        builder.field("relocating_shards", this.zoneAgg.relocatingShards);
        builder.field("unassigned_shards", this.zoneAgg.unassignedShards);
        builder.endObject();

        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        return true;

    }

    @Override
    public int hashCode() {
        return Objects.hash(
                clusterHealthResponse
        );
    }

    @Override
    public RestStatus status() {
        return clusterHealthResponse.status();
    }

    @Override
    public boolean isFragment() {
        return StatusToXContentObject.super.isFragment();
    }
}

