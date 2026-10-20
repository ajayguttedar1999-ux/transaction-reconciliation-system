package com.recon.cache.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recon.cache.model.CachedPosition;
import com.recon.common.event.PositionCreatedEvent;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.time.Instant;

@ApplicationScoped
public class PositionCacheConsumer {

    private static final Logger log = Logger.getLogger(PositionCacheConsumer.class);

    private final ValueCommands<String, String> valueCommands;
    private final ObjectMapper objectMapper;

    public PositionCacheConsumer(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.valueCommands = redisDataSource.value(String.class);
        this.objectMapper = objectMapper;
    }

    @Incoming("positions-in")
    public void onPositionCreated(String rawJson) {
        try {
            PositionCreatedEvent event = objectMapper.readValue(rawJson, PositionCreatedEvent.class);
            log.infof("Quarkus received position event for product [%s]: totalQty=%s", 
                    event.productId(), event.totalQuantity());

            CachedPosition cached = new CachedPosition(
                    event.positionId(),
                    event.productId(),
                    event.totalQuantity(),
                    event.totalAmount(),
                    event.version(),
                    Instant.now()
            );

            String redisKey = "pos:" + event.productId();
            String jsonPayload = objectMapper.writeValueAsString(cached);

            // Store in Redis in < 1 millisecond!
            valueCommands.set(redisKey, jsonPayload);
            log.infof("⚡ Cached in Redis key [%s] successfully: %s", redisKey, jsonPayload);

        } catch (Exception ex) {
            log.errorf("Error caching position from Kafka: %s", ex.getMessage(), ex);
        }
    }
}
