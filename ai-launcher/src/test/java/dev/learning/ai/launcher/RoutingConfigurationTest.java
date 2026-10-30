package dev.learning.ai.launcher;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RoutingConfigurationTest {
    @Test void invalidRoutesFailDuringAssembly() {
        var app = new AiApplication();
        for (var route : List.of(
                new RoutingProperties.Route("bad", "unsupported", null, null, null),
                new RoutingProperties.Route("bad", null, null, null, null),
                new RoutingProperties.Route("bad", "ollama", "", null, null),
                new RoutingProperties.Route("bad", "ollama", "model", null, 0L),
                new RoutingProperties.Route("bad", "ollama", "model", "file:///tmp/model", 1L))) {
            assertThrows(IllegalArgumentException.class, () -> app.chatModel(
                    new RoutingProperties(List.of(route)), "mock", "http://127.0.0.1:11434", "", 30));
        }
        var duplicate = new RoutingProperties.Route("same", "mock", null, null, null);
        assertThrows(IllegalArgumentException.class, () -> app.chatModel(
                new RoutingProperties(List.of(duplicate, duplicate)), "mock", "http://127.0.0.1:11434", "", 30));
    }
}
