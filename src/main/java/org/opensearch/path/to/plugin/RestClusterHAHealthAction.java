package org.opensearch.path.to.plugin;

import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.client.node.NodeClient;
import org.opensearch.common.logging.DeprecationLogger;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.action.RestStatusToXContentListener;
import org.opensearch.rest.action.admin.cluster.RestClusterHealthAction;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.opensearch.rest.RestRequest.Method.GET;


public class RestClusterHAHealthAction extends BaseRestHandler {



    private static final DeprecationLogger deprecationLogger = DeprecationLogger.getLogger(RestClusterHAHealthAction.class);

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(new Route(GET, "/_cluster/ha_health"), new Route(GET, "/_cluster/ha_health/{index}")));
    }

    @Override
    public String getName() {
        return "cluster_ha_health_action";
    }

    @Override
    public boolean allowSystemIndexAccessByDefault() {
        return true;
    }

    @Override
    public RestChannelConsumer prepareRequest(final RestRequest request, final NodeClient client) throws IOException {
        final ClusterHealthRequest clusterHealthRequest = fromRequest(request);
        return channel -> client.admin().cluster().health(clusterHealthRequest, new RestStatusToXContentListener<>(channel));
    }

    public static ClusterHealthRequest fromRequest(final RestRequest request) {
        final ClusterHealthRequest clusterHealthRequest = RestClusterHealthAction.fromRequest(request);
        return clusterHealthRequest;
    }

    private static final Set<String> RESPONSE_PARAMS = Collections.singleton("level");

    @Override
    protected Set<String> responseParams() {
        return RESPONSE_PARAMS;
    }

    @Override
    public boolean canTripCircuitBreaker() {
        return false;
    }

}
