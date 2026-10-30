package dev.learning.ai.launcher;

import dev.learning.ai.core.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ObservationFilterTest {
    private RequestContext context(String alias) {
        return new RequestContext("private-id", new ChatRequest(alias, List.of(new Message("user", "secret"))));
    }
    private ModelRouter router(ChatModel model) {
        return new ModelRouter(List.of(new ModelRouter.Route(
                new ModelRouter.Descriptor("assistant", "test", false), model)));
    }

    @Test void onlyRecordsReportedUsageAndPreservesMissingOrSimulatedValues() {
        var registry = new SimpleMeterRegistry();
        try {
            var router = router(r -> null);
            var filter = new ObservationFilter(router, registry);
            for (var usage : List.of(ChatResult.Usage.unavailable(), new ChatResult.Usage(8L, 9L, "estimated"))) {
                filter.apply(context("assistant"), c -> new ChatResult("test", "test", "ok", usage, false));
            }
            filter.apply(context("assistant"), c -> new ChatResult("mock", "test", "ok",
                    new ChatResult.Usage(100L, 100L, "provider"), true));
            assertTrue(registry.find("ai.gateway.tokens").counters().isEmpty());
            filter.apply(context("assistant"), c -> new ChatResult("test", "test", "ok",
                    new ChatResult.Usage(7L, null, "provider"), false));
            assertEquals(7, registry.get("ai.gateway.tokens").tag("direction", "input").counter().count());
            assertNull(registry.find("ai.gateway.tokens").tag("direction", "output").counter());
            assertEquals(4, registry.get("ai.gateway.calls").tag("outcome", "success").timer().count());
        } finally { registry.close(); }
    }

    @Test void failuresAndUnknownAliasesHaveBoundedMetricTags() {
        var registry = new SimpleMeterRegistry();
        try {
            var router = router(r -> { throw new ProviderException(); });
            var pipeline = new GatewayPipeline(List.of(new ObservationFilter(router, registry)), router);
            assertThrows(ProviderException.class, () -> pipeline.execute(context("assistant")));
            for (int i = 0; i < 20; i++) {
                int n = i;
                assertThrows(UnknownModelException.class, () -> pipeline.execute(context("unknown-" + n)));
            }
            assertEquals(1, registry.get("ai.gateway.calls").tag("outcome", "error").timer().count());
            assertEquals(20, registry.get("ai.gateway.calls").tag("model", "_unknown").timer().count());
            assertEquals(2, registry.getMeters().size());
        } finally { registry.close(); }
    }
}
