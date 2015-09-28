package io.vertxbeans;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.metrics.MetricsOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Created by Rob Worsnop on 9/5/15.
 */
public class VertxBeansBase {
    @Autowired(required = false)
    private ClusterManager clusterManager;

    @Autowired(required = false)
    private MetricsOptions metricsOptions;

    @Bean
    protected VertxOptions vertxOptions(
            @Value("${vertx.warning-exception-time:}") Long warningExceptionTime,
            @Value("${vertx.event-loop-pool-size:}") Integer eventLoopPoolSize,
            @Value("${vertx.max-event-loop-execution-time:}") Long maxEventLoopExecutionTime,
            @Value("${vertx.worker-pool-size:}") Integer workerPoolSize,
            @Value("${vertx.max-worker-execution-time:}") Long maxWorkerExecutionTime,
            @Value("${vertx.blocked-thread-check-interval:}") Long blockedThreadCheckInterval,
            @Value("${vertx.internal-blocking-pool-size:}") Integer internalBlockingPoolSize,
            @Value("${vertx.ha-enabled:false}") boolean haEnabled,
            @Value("${vertx.ha-group:}") String haGroup,
            @Value("${vertx.quorum-size:}") Integer quorumSize,
            @Value("${vertx.cluster-host:localhost}") String clusterHost,
            @Value("${vertx.cluster-port:}") Integer clusterPort,
            @Value("${vertx.cluster-ping-interval:}") Long clusterPingInterval,
            @Value("${vertx.cluster-ping-reply-interval:}") Long clusterPingReplyInterval,
            @Value("${vertx.clustered:false}") boolean clustered) {
        VertxOptions options = new VertxOptions();

        setParameter(warningExceptionTime, options::setWarningExceptionTime);
        setParameter(eventLoopPoolSize, options::setEventLoopPoolSize);
        setParameter(maxEventLoopExecutionTime, options::setMaxEventLoopExecuteTime);
        setParameter(workerPoolSize, options::setWorkerPoolSize);
        setParameter(maxWorkerExecutionTime, options::setMaxWorkerExecuteTime);
        setParameter(blockedThreadCheckInterval, options::setBlockedThreadCheckInterval);
        setParameter(internalBlockingPoolSize, options::setInternalBlockingPoolSize);
        options.setHAEnabled(haEnabled);
        setParameter(haGroup, options::setHAGroup);
        setParameter(quorumSize, options::setQuorumSize);
        options.setClustered(clustered);
        options.setClusterHost(clusterHost);
        setParameter(clusterPort, options::setClusterPort);
        setParameter(clusterPingInterval, options::setClusterPingInterval);
        setParameter(clusterPingReplyInterval, options::setClusterPingReplyInterval);
        setParameter(clusterManager, options::setClusterManager);
        setParameter(metricsOptions, options::setMetricsOptions);

        return options;
    }

    protected <T> T clusteredVertx(Consumer<Handler<AsyncResult<T>>> consumer) throws Throwable {
        AtomicReference<AsyncResult<T>> result = new AtomicReference<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        clusteredVertx(consumer, ar -> {
            result.set(ar);
            countDownLatch.countDown();
        });
        if (!countDownLatch.await(2, MINUTES)) {
            throw new RuntimeException("Timed out trying to join cluster!");
        }
        AsyncResult<T> ar = result.get();
        if (ar.succeeded()) {
            return ar.result();
        } else {
            throw ar.cause();
        }
    }

    private <T> void clusteredVertx(Consumer<Handler<AsyncResult<T>>> consumer, Handler<AsyncResult<T>> handler){
        consumer.accept(handler);
    }

    private <T> void setParameter(T param, Consumer<T> setter){
        if (param != null){
            setter.accept(param);
        }
    }

}