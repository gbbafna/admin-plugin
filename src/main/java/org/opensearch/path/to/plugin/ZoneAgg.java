package org.opensearch.path.to.plugin;

import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.opensearch.cluster.health.ClusterStateHealth;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.unit.TimeValue;

import java.io.IOException;

public class ZoneAgg {
    int numNodes;
    int shardCount;

     int activeShards;
     int relocatingShards;
     int initializingShards;
    int unassignedShards;
    String name;

    ZoneAgg(String name) {
        this.name = name;
        this.numNodes = 0;
        this.shardCount = 0;
        this.activeShards = 0;
        this.relocatingShards = 0;
        this.initializingShards = 0;
        this.unassignedShards = 0;
    }

    public ZoneAgg(StreamInput in) throws IOException {
        this.name = in.readString();
        this.numNodes = in.readInt();
        this.shardCount = in.readInt();
        this.activeShards = in.readInt();
        this.relocatingShards = in.readInt();
        this.initializingShards = in.readInt();
        this.unassignedShards = in.readInt();
    }

    public static ZoneAgg readResponseFrom(StreamInput in) throws IOException {
        return new ZoneAgg(in);
    }

    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
        out.writeInt(numNodes);
        out.writeInt(shardCount);
        out.writeInt(activeShards);
        out.writeInt(relocatingShards);
        out.writeInt(initializingShards);
        out.writeInt(unassignedShards);
    }

}
