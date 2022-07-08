package org.opensearch.path.to.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.opensearch.action.ActionListener;
import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.action.admin.cluster.health.TransportClusterHealthAction;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.ActiveShardCount;
import org.opensearch.action.support.IndicesOptions;
import org.opensearch.action.support.master.TransportMasterNodeReadAction;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.ClusterStateObserver;
import org.opensearch.cluster.ClusterStateUpdateTask;
import org.opensearch.cluster.LocalClusterUpdateTask;
import org.opensearch.cluster.NotMasterException;
import org.opensearch.cluster.block.ClusterBlockException;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.metadata.ProcessClusterEventTimeoutException;
import org.opensearch.cluster.node.DiscoveryNode;
import org.opensearch.cluster.routing.RoutingNode;
import org.opensearch.cluster.routing.RoutingTable;
import org.opensearch.cluster.routing.ShardRoutingState;
import org.opensearch.cluster.routing.UnassignedInfo;
import org.opensearch.cluster.routing.allocation.AllocationService;
import org.opensearch.cluster.routing.allocation.decider.AwarenessAllocationDecider;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.Strings;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.util.CollectionUtils;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.node.NodeClosedException;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.opensearch.cluster.routing.allocation.decider.AwarenessAllocationDecider.CLUSTER_ROUTING_ALLOCATION_AWARENESS_ATTRIBUTE_SETTING;
import static org.opensearch.cluster.routing.allocation.decider.AwarenessAllocationDecider.CLUSTER_ROUTING_ALLOCATION_AWARENESS_FORCE_GROUP_SETTING;


public class TransportClusterHAHealthAction extends TransportMasterNodeReadAction<ClusterHealthRequest, ClusterHAHealthResponse> {

    private static final Logger logger = LogManager.getLogger(TransportClusterHAHealthAction.class);

    private final AllocationService allocationService;

    private TransportClusterHealthAction transportClusterHealthAction;

    @Inject
    public TransportClusterHAHealthAction(
            TransportService transportService,
            ClusterService clusterService,
            ThreadPool threadPool,
            ActionFilters actionFilters,
            IndexNameExpressionResolver indexNameExpressionResolver,
            AllocationService allocationService,
            TransportClusterHealthAction transportClusterHealthAction
    ) {
        super(
                ClusterHAHealthAction.NAME,
                false,
                transportService,
                clusterService,
                threadPool,
                actionFilters,
                ClusterHealthRequest::new,
                indexNameExpressionResolver
        );
        this.allocationService = allocationService;
        this.transportClusterHealthAction = transportClusterHealthAction;
    }

    @Override
    protected String executor() {
        // this should be executing quickly no need to fork off
        return ThreadPool.Names.SAME;
    }

    @Override
    protected ClusterHAHealthResponse read(StreamInput in) throws IOException {
        return new ClusterHAHealthResponse(in);
    }

    @Override
    protected ClusterBlockException checkBlock(ClusterHealthRequest request, ClusterState state) {
        // we want users to be able to call this even when there are global blocks, just to check the health (are there blocks?)
        return null;
    }

    @Override
    protected final void masterOperation(ClusterHealthRequest request, ClusterState state, ActionListener<ClusterHAHealthResponse> listener)
            throws Exception {
        logger.warn("attempt to execute a cluster health operation without a task");
        throw new UnsupportedOperationException("task parameter is required for this operation");
    }

    @Override
    protected void masterOperation(
            final Task task,
            final ClusterHealthRequest request,
            final ClusterState unusedState,
            final ActionListener<ClusterHAHealthResponse> listener
    ) {

        ActionListener<ClusterHealthResponse> listener2 = new ActionListener<>() {
            @Override
            public void onResponse(ClusterHealthResponse clusterHealthResponse) {
                ClusterHAHealthResponse clusterHAHealthResponse = new ClusterHAHealthResponse(clusterHealthResponse);
                Settings forceSettings = CLUSTER_ROUTING_ALLOCATION_AWARENESS_FORCE_GROUP_SETTING.get(clusterService.getSettings());
                Map<String, List<String>> forcedAwarenessAttributes = new HashMap<>();
                Map<String, Settings> forceGroups = forceSettings.getAsGroups();
                for (Map.Entry<String, Settings> entry : forceGroups.entrySet()) {
                    List<String> aValues = entry.getValue().getAsList("values");
                    if (aValues.size() > 0) {
                        forcedAwarenessAttributes.put(entry.getKey(), aValues);
                    }
                }
                logger.info("List of zones", forcedAwarenessAttributes.get("zone"));
                // Find out assigned shards in each zone
                logger.info("gbbafna aa gaya " + clusterHealthResponse);

                Map<String, ZoneAgg> zoneHealth = new HashMap<>();
                Map<String , String> nodeIdToZone = new HashMap<>();

                Iterator<DiscoveryNode> it =  clusterService.state().getNodes().iterator();
                while (it.hasNext()) {
                    DiscoveryNode node = it.next();
                    String z = node.getAttributes().get("zone");
                    ZoneAgg zh = zoneHealth.computeIfAbsent(z, k -> new ZoneAgg(z));
                    zh.numNodes++;
                    zoneHealth.put(z, zh);
                    nodeIdToZone.put(node.getId(), z);
                }

                Iterator<RoutingNode> rt = clusterService.state().getRoutingNodes().iterator();
                while (rt.hasNext())  {
                    RoutingNode rn = rt.next();
                    String z = rn.node().getAttributes().get("zone");
                    ZoneAgg zh = zoneHealth.get(z);
                    zh.shardCount += rn.size();
                    zh.activeShards += rn.numberOfOwningShards();
                    zh.initializingShards += rn.numberOfShardsWithState(ShardRoutingState.INITIALIZING);
                    zh.relocatingShards += rn.numberOfShardsWithState(ShardRoutingState.RELOCATING);
                }

                int totalShards = clusterHealthResponse.getActiveShards() - clusterHealthResponse.getRelocatingShards() + clusterHealthResponse.getInitializingShards();
                logger.info("Total shards are " + totalShards);
                Iterator<ZoneAgg> zhI = zoneHealth.values().iterator();
                while (zhI.hasNext()) {
                    ZoneAgg k = zhI.next();
                    clusterHAHealthResponse.zoneAgg = k;
                    k.unassignedShards = totalShards/zoneHealth.size() - k.activeShards - k.initializingShards;
                    logger.info("For zone" + k.name + " shard count" + k.shardCount + "node count" + k.numNodes);
                    logger.info("ZG is " + k);
                }

                listener.onResponse(clusterHAHealthResponse);
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        };

        this.transportClusterHealthAction.execute(task, request, listener2);
    }


}

