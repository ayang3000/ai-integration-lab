package dev.learning.ai.launcher;

import dev.learning.ai.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;

final class ObservationFilter implements GatewayFilter {
    private static final Logger log = LoggerFactory.getLogger(ObservationFilter.class);
    private final ModelRouter router;
    private final MeterRegistry registry;

    ObservationFilter(ModelRouter router, MeterRegistry registry) {
        this.router = router;
        this.registry = registry;
    }

    @Override public ChatResult apply(RequestContext context, Chain next) {
        long started = System.nanoTime();
        String outcome = "error";
        var route = router.describe(context.request().model());
        // Never use arbitrary client input as a metric tag.
        String alias = route.map(ModelRouter.Descriptor::alias).orElse("_unknown");
        String provider = route.map(ModelRouter.Descriptor::provider).orElse("none");
        try {
            ChatResult result = next.proceed(context);
            outcome = "success";
            if (!result.simulated() && result.usage() != null && "provider".equals(result.usage().source())) {
                recordTokens(alias, provider, "input", result.usage().inputTokens());
                recordTokens(alias, provider, "output", result.usage().outputTokens());
            }
            return result;
        } catch (UnknownModelException failure) {
            outcome = "rejected";
            throw failure;
        } finally {
            registry.timer("ai.gateway.calls", "model", alias, "provider", provider, "outcome", outcome)
                    .record(System.nanoTime() - started, TimeUnit.NANOSECONDS);
            log.info("event=model_call requestId={} outcome={} durationMs={}", context.requestId(),
                    outcome, (System.nanoTime() - started) / 1_000_000);
        }
    }

    private void recordTokens(String alias, String provider, String direction, Long count) {
        if (count != null && count >= 0) {
            registry.counter("ai.gateway.tokens", "model", alias, "provider", provider,
                    "direction", direction).increment(count);
        }
    }
}
