package dev.learning.ai.providers;

import dev.learning.ai.core.*;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/** A deliberately small synchronous adapter; streaming is a separate learning milestone. */
public final class OllamaChatModel implements ChatModel {
    private final RestClient client;
    private final String model;

    public OllamaChatModel(String baseUrl, String model, Duration timeout) {
        URI uri = URI.create(baseUrl);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Invalid Ollama base URL");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("OLLAMA_MODEL is required");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("Invalid timeout");
        this.model = model;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(timeout);
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override public ChatResult chat(ChatRequest request) {
        try {
            Reply reply = client.post().uri("/api/chat")
                    .body(new Payload(model, request.messages(), false))
                    .retrieve().body(Reply.class);
            if (reply == null || !Boolean.TRUE.equals(reply.done()) || reply.message() == null
                    || !"assistant".equals(reply.message().role()) || reply.message().content() == null
                    || reply.message().content().isBlank()
                    || negative(reply.prompt_eval_count()) || negative(reply.eval_count()))
                throw new ProviderException();
            String source = reply.prompt_eval_count() == null && reply.eval_count() == null
                    ? "unavailable" : "provider";
            return new ChatResult("ollama", model, reply.message().content(),
                    new ChatResult.Usage(reply.prompt_eval_count(), reply.eval_count(), source), false);
        } catch (ProviderException e) { throw e; }
        catch (RuntimeException e) { throw new ProviderException(); }
    }

    private static boolean negative(Long value) { return value != null && value < 0; }
    public record Payload(String model, List<Message> messages, boolean stream) {}
    public record Reply(ReplyMessage message, Boolean done, Long prompt_eval_count, Long eval_count) {}
    public record ReplyMessage(String role, String content) {}
}
