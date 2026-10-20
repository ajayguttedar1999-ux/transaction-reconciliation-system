package com.recon.cache.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recon.cache.model.CachedPosition;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/api/v1/cache")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PositionCacheResource {

    private static final Logger log = Logger.getLogger(PositionCacheResource.class);

    private final ValueCommands<String, String> valueCommands;
    private final ObjectMapper objectMapper;

    public PositionCacheResource(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.valueCommands = redisDataSource.value(String.class);
        this.objectMapper = objectMapper;
    }

    /**
     * Ultra-low-latency endpoint: reads directly from in-memory Redis!
     * Response time: typically < 2ms (vs 25ms from relational database).
     */
    @GET
    @Path("/positions/{productId}")
    public Response getCachedPosition(@PathParam("productId") String productId) {
        long startTime = System.nanoTime();
        String redisKey = "pos:" + productId;

        String rawJson = valueCommands.get(redisKey);
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        if (rawJson == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of(
                            "error", "Position not found in cache",
                            "productId", productId,
                            "lookupTimeMs", durationMs
                    ))
                    .build();
        }

        try {
            CachedPosition cached = objectMapper.readValue(rawJson, CachedPosition.class);
            log.infof("Served cache for [%s] in %d ms", productId, durationMs);
            return Response.ok(cached)
                    .header("X-Response-Time-Ms", durationMs)
                    .header("X-Cache-Source", "REDIS")
                    .build();
        } catch (Exception ex) {
            return Response.serverError()
                    .entity(Map.of("error", "Failed to deserialize cached position"))
                    .build();
        }
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of(
                "status", "UP",
                "service", "position-cache-service",
                "framework", "Quarkus 3.15",
                "cache", "Redis 7"
        )).build();
    }
}
