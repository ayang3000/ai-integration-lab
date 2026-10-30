package dev.learning.ai.launcher;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "ai.routing")
public record RoutingProperties(List<Route> routes) {
    public RoutingProperties {
        routes = routes == null ? List.of() : List.copyOf(routes);
    }

    public record Route(String alias, String provider, String model, String baseUrl, Long timeoutSeconds) {}
}
