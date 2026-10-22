package dev.learning.ai.providers;

import dev.learning.ai.core.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import tools.jackson.databind.json.JsonMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class OllamaChatModelTest {
    private HttpServer server;
    private final AtomicReference<String> captured = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();
    private volatile String body;
    private volatile int status = 200;
    private volatile long delayMs;
    private final ChatRequest request = new ChatRequest("assistant", List.of(new Message("user", "你好")));
    @BeforeEach void start() throws Exception {
        body = "{\"message\":{\"role\":\"assistant\",\"content\":\"hello\"},\"done\":true,\"prompt_eval_count\":8,\"eval_count\":2}";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            calls.incrementAndGet();
            if (delayMs > 0) {
                try { Thread.sleep(delayMs); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            captured.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    private OllamaChatModel model() {
        return new OllamaChatModel("http://127.0.0.1:" + server.getAddress().getPort(), "installed-model", Duration.ofSeconds(2));
    }
    @Test void mapsRealWireContractAndNeverRetries() {
        ChatResult result = model().chat(request);
        var payload = JsonMapper.builder().build().readTree(captured.get());
        assertEquals("installed-model", payload.path("model").asText());
        assertFalse(payload.path("stream").asBoolean());
        assertEquals("你好", payload.path("messages").get(0).path("content").asText());
        assertEquals("hello", result.content());
        assertEquals(8L, result.usage().inputTokens());
        assertEquals(2L, result.usage().outputTokens());
        assertFalse(result.simulated());
        assertEquals(1, calls.get());
    }
    @Test void missingUsageStaysUnknown() {
        body = "{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"done\":true}";
        assertEquals(ChatResult.Usage.unavailable(), model().chat(request).usage());
    }
    @Test void errorsDoNotLeakProviderPayloadOrRetry() {
        status = 500; body = "{\"error\":\"secret prompt or token\"}";
        ProviderException e = assertThrows(ProviderException.class, () -> model().chat(request));
        assertEquals("Model provider failed", e.getMessage());
        assertNull(e.getCause());
        assertEquals(1, calls.get());
    }
    @Test void slowProviderIsBoundedByReadTimeout() {
        delayMs = 400;
        var slow = new OllamaChatModel("http://127.0.0.1:" + server.getAddress().getPort(),
                "installed-model", Duration.ofMillis(100));
        assertThrows(ProviderException.class, () -> slow.chat(request));
        assertEquals(1, calls.get());
    }
    @Test void rejectsMalformedOrIncompleteSuccess() {
        body = "{\"message\":{\"role\":\"assistant\",\"content\":\"partial\"},\"done\":false}";
        assertThrows(ProviderException.class, () -> model().chat(request));
        body = "not-json";
        assertThrows(ProviderException.class, () -> model().chat(request));
    }
}
