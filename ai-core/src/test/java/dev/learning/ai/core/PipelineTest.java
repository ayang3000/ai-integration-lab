package dev.learning.ai.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PipelineTest {
    private final RequestContext ctx = new RequestContext("test-id",
            new ChatRequest("assistant", List.of(new Message("user", "hello"))));
    private final ChatResult result = new ChatResult("test", "test", "ok", ChatResult.Usage.unavailable(), true);
    private GatewayFilter filter(String name, List<String> events) {
        return (context, next) -> {
            events.add(name + "+");
            try { return next.proceed(context); }
            finally { events.add(name + "-"); }
        };
    }

    @Test void entersInOrderAndUnwindsInReverseOrder() {
        List<String> events = new ArrayList<>();
        GatewayPipeline pipeline = new GatewayPipeline(List.of(filter("A", events), filter("B", events)),
                request -> { events.add("model"); return result; });
        assertSame(result, pipeline.execute(ctx));
        assertEquals(List.of("A+", "B+", "model", "B-", "A-"), events);
    }
    @Test void cleansUpOnProviderFailureWithoutChangingOriginalFailure() {
        List<String> events = new ArrayList<>();
        ProviderException failure = new ProviderException();
        GatewayPipeline pipeline = new GatewayPipeline(List.of(filter("A", events), filter("B", events)),
                request -> { throw failure; });
        assertSame(failure, assertThrows(ProviderException.class, () -> pipeline.execute(ctx)));
        assertEquals(List.of("A+", "B+", "B-", "A-"), events);
    }
    @Test void shortCircuitNeverEntersLaterFiltersOrProvider() {
        GatewayFilter deny = (context, next) -> result;
        GatewayFilter never = (context, next) -> { fail("later filter entered"); return null; };
        GatewayPipeline pipeline = new GatewayPipeline(List.of(deny, never), request -> {
            fail("provider called"); return null;
        });
        assertSame(result, pipeline.execute(ctx));
    }
    @Test void copiesInputMessagesAndEnforcesAggregateBudget() {
        List<Message> messages = new ArrayList<>(List.of(new Message("user", "hello")));
        ChatRequest request = new ChatRequest("assistant", messages);
        messages.clear();
        assertEquals(1, request.messages().size());
        assertThrows(UnsupportedOperationException.class, () -> request.messages().clear());
        assertThrows(IllegalArgumentException.class, () -> new ChatRequest("assistant",
                Collections.nCopies(3, new Message("user", "x".repeat(16000)))));
    }
}
