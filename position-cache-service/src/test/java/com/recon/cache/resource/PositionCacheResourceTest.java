package com.recon.cache.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class PositionCacheResourceTest {

    @Test
    public void testHealthEndpoint() {
        given()
          .when().get("/api/v1/cache/health")
          .then()
             .statusCode(200)
             .body("status", is("UP"))
             .body("service", is("position-cache-service"));
    }
}
