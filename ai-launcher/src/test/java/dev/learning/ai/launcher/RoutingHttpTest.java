package dev.learning.ai.launcher;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"ai.provider=mock"})
class RoutingHttpTest {
    private static final AtomicInteger calls = new AtomicInteger();
    private static final HttpServer upstream = startUpstream();
    private static volatile String payload;
    @Value("${local.server.port}") int port;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    private static HttpServer startUpstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/chat", exchange -> {
                calls.incrementAndGet();
                payload = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] response = "{\"done\":true,\"message\":{\"role\":\"assistant\",\"content\":\"fixture answer\"},\"prompt_eval_count\":7,\"eval_count\":3}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    @DynamicPropertySource static void upstreamUrl(DynamicPropertyRegistry registry) {
        registry.add("ai.routing.routes[0].alias", () -> "offline");
        registry.add("ai.routing.routes[0].provider", () -> "mock");
        registry.add("ai.routing.routes[1].alias", () -> "incident");
        registry.add("ai.routing.routes[1].provider", () -> "ollama");
        registry.add("ai.routing.routes[1].model", () -> "fixture-model");
        registry.add("ai.routing.routes[1].timeout-seconds", () -> "2");
        registry.add("ai.routing.routes[1].base-url", () -> "http://127.0.0.1:" + upstream.getAddress().getPort());
    }
    @AfterAll static void stopUpstream() { upstream.stop(0); }
    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> chat(String alias) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"model\":\"" + alias + "\",\"messages\":[{\"role\":\"user\",\"content\":\"synthetic incident\"}]}"))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void discoversAndRoutesAcrossProvidersWithoutLeakingUpstreamConfiguration() throws Exception {
        int before = calls.get();
        var catalog = get("/api/models");
        assertEquals(200, catalog.statusCode());
        assertEquals(2, json.readTree(catalog.body()).size());
        assertFalse(catalog.body().contains("127.0.0.1"));
        assertFalse(catalog.body().contains("fixture-model"));
        assertEquals(before, calls.get()); // Catalog does not probe the model.
        var offline = chat("offline");
        assertEquals(200, offline.statusCode());
        assertTrue(json.readTree(offline.body()).path("simulated").asBoolean());
        assertEquals(before, calls.get());
        var incident = chat("incident");
        assertEquals(200, incident.statusCode());
        var body = json.readTree(incident.body());
        assertEquals("incident", body.path("model").asText());
        assertEquals("fixture-model", body.path("upstreamModel").asText());
        assertFalse(body.path("simulated").asBoolean());
        assertEquals(7, body.path("usage").path("inputTokens").asInt());
        assertEquals("fixture-model", json.readTree(payload).path("model").asText());
        assertFalse(json.readTree(payload).path("stream").asBoolean());
        assertEquals(before + 1, calls.get());
        assertEquals(404, chat("assistant").statusCode()); // Explicit routes replace legacy default.
        assertEquals(404, chat("fixture-model").statusCode());
        assertEquals(before + 1, calls.get());
        assertEquals(200, get("/actuator/metrics/ai.gateway.calls").statusCode());
        assertEquals(200, get("/actuator/metrics/ai.gateway.tokens").statusCode());
    }
}
