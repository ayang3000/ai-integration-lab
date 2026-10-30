package dev.learning.ai.launcher;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.http.*;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"ai.provider=mock"})
class ChatHttpTest {
    @Value("${local.server.port}") int port;
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpResponse<String> post(String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private String input(String model, String role) {
        return "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"" + role + "\",\"content\":\"hello\"}]}";
    }
    @Test void mockIsExplicitAndRequestIdIsServerGenerated() throws Exception {
        var response = post(input("assistant", "user"));
        assertEquals(200, response.statusCode());
        var json = JsonMapper.builder().build().readTree(response.body());
        assertTrue(json.path("simulated").asBoolean());
        assertEquals("unavailable", json.path("usage").path("source").asText());
        assertTrue(json.path("usage").path("inputTokens").isNull());
        assertEquals(json.path("requestId").asText(), response.headers().firstValue("X-Request-Id").orElseThrow());
        assertNotEquals(json.path("requestId").asText(),
                JsonMapper.builder().build().readTree(post(input("assistant", "user")).body()).path("requestId").asText());
    }
    @Test void unknownAliasAndInvalidRoleAreRejected() throws Exception {
        assertEquals(404, post(input("arbitrary-upstream-model", "user")).statusCode());
        assertEquals(400, post(input("assistant", "system")).statusCode());
    }
    @Test void malformedEmptyAndNullMessagesAreSafeErrors() throws Exception {
        for (String body : new String[]{"{", "{}", "{\"model\":\"assistant\",\"messages\":[]}",
                "{\"model\":\"assistant\",\"messages\":[null]}"}) {
            var response = post(body);
            assertEquals(400, response.statusCode());
            var json = JsonMapper.builder().build().readTree(response.body());
            assertEquals("INVALID_REQUEST", json.path("code").asText());
            assertFalse(json.path("requestId").asText().isBlank());
        }
    }
    @Test void preservesFrameworkNotFoundAndMethodStatuses() throws Exception {
        for (String path : new String[]{"/missing", "/api/chat"}) {
            var response = client.send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(path.equals("/missing") ? 404 : 405, response.statusCode());
            assertFalse(response.body().contains("stackTrace"));
        }
    }

    @Test void adminPortalAndAssetsAreServedAlongsideApi() throws Exception {
        for (String path : new String[]{"/", "/admin/styles.css", "/admin/app.mjs", "/admin/core.mjs"}) {
            var response = client.send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), path);
            String type = response.headers().firstValue("Content-Type").orElseThrow();
            if (path.equals("/")) {
                assertTrue(type.contains("text/html"));
                assertTrue(response.body().contains("Admin Portal"));
            } else if (path.endsWith(".mjs")) {
                assertTrue(type.contains("javascript"), type);
            } else {
                assertTrue(type.contains("text/css"), type);
            }
            assertFalse(response.headers().firstValue("X-Request-Id").orElseThrow().isBlank());
        }
    }

}
