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
import org.opensearch.cluster.routing.UnassignedInfo;
import org.opensearch.cluster.routing.allocation.AllocationService;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.Strings;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.util.CollectionUtils;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.node.NodeClosedException;
import org.opensearch.tasks.Task;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;


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
                logger.info("gbbafna aa gaya " + clusterHealthResponse);
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

