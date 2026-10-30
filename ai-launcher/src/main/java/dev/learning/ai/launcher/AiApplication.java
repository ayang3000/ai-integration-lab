package dev.learning.ai.launcher;

import dev.learning.ai.core.*;
import dev.learning.ai.providers.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties(RoutingProperties.class)
public class AiApplication {
    public static void main(String[] args) { SpringApplication.run(AiApplication.class, args); }

    @Bean ModelRouter chatModel(RoutingProperties routing,
                             @Value("${ai.provider}") String provider,
                             @Value("${ai.ollama.base-url}") String baseUrl,
                             @Value("${ai.ollama.model}") String model,
                             @Value("${ai.ollama.timeout-seconds}") long seconds) {
        List<RoutingProperties.Route> routes = routing.routes().isEmpty()
                ? List.of(new RoutingProperties.Route("assistant", provider, model, baseUrl, seconds))
                : routing.routes();
        return new ModelRouter(routes.stream().map(route -> {
            String selected = route.provider();
            if (selected == null) throw new IllegalArgumentException("Route provider is required");
            ChatModel target = switch (selected) {
            case "mock" -> new MockChatModel();
            case "ollama" -> new OllamaChatModel(
                    route.baseUrl() == null ? baseUrl : route.baseUrl(),
                    route.model() == null ? model : route.model(),
                    Duration.ofSeconds(route.timeoutSeconds() == null ? seconds : route.timeoutSeconds()));
            default -> throw new IllegalArgumentException("Unsupported route provider");
            };
            return new ModelRouter.Route(new ModelRouter.Descriptor(route.alias(), selected,
                    "mock".equals(selected)), target);
        }).toList());
    }

    @Bean GatewayPipeline gatewayPipeline(ChatModel model, ModelRouter router, MeterRegistry registry) {
        return new GatewayPipeline(List.of(new ObservationFilter(router, registry)), model);
    }
}
