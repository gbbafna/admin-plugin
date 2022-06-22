package org.opensearch.path.to.plugin;

import org.opensearch.action.ActionType;
import org.opensearch.action.admin.cluster.health.ClusterHealthAction;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.common.io.stream.StreamInput;


public class ClusterHAHealthAction extends ActionType<ClusterHAHealthResponse> {

    public static final ClusterHAHealthAction INSTANCE = new ClusterHAHealthAction();
    public static final String NAME = "cluster:monitor/ha_health";

    private ClusterHAHealthAction() {
        super(NAME, ClusterHAHealthResponse::new);
    }

}

