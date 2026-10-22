package dev.learning.ai.launcher;

import dev.learning.ai.core.*;
import dev.learning.ai.providers.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.util.List;

@SpringBootApplication
public class AiApplication {
    public static void main(String[] args) { SpringApplication.run(AiApplication.class, args); }

    @Bean ChatModel chatModel(@Value("${ai.provider}") String provider,
                             @Value("${ai.ollama.base-url}") String baseUrl,
                             @Value("${ai.ollama.model}") String model,
                             @Value("${ai.ollama.timeout-seconds}") long seconds) {
        return switch (provider) {
            case "mock" -> new MockChatModel();
            case "ollama" -> new OllamaChatModel(baseUrl, model, Duration.ofSeconds(seconds));
            default -> throw new IllegalArgumentException("Unsupported AI_PROVIDER");
        };
    }

    @Bean GatewayPipeline gatewayPipeline(ChatModel model) {
        return new GatewayPipeline(List.of(new ObservationFilter()), model);
    }
}
