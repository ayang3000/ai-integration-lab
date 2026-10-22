package dev.learning.ai.launcher;

import dev.learning.ai.core.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {AiApplication.class, ProviderFailureHttpTest.FailingProvider.class},
        properties = {"ai.provider=mock"})
class ProviderFailureHttpTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class FailingProvider {
        @Bean @Primary ChatModel failingModel() { return request -> { throw new ProviderException(); }; }
    }
    @Value("${local.server.port}") int port;
    @Test void providerFailureReturnsSafeErrorAndRequestId() throws Exception {
        String input = "{\"model\":\"assistant\",\"messages\":[{\"role\":\"user\",\"content\":\"private-content-marker\"}]}";
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(input)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(502, response.statusCode());
        assertTrue(response.body().contains("PROVIDER_FAILURE"));
        assertFalse(response.body().contains("private-content-marker"));
        assertTrue(response.body().contains(response.headers().firstValue("X-Request-Id").orElseThrow()));
    }
}
