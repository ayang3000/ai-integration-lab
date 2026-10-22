package dev.learning.ai.launcher;

import dev.learning.ai.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ObservationFilter implements GatewayFilter {
    private static final Logger log = LoggerFactory.getLogger(ObservationFilter.class);

    @Override public ChatResult apply(RequestContext context, Chain next) {
        long started = System.nanoTime();
        String outcome = "error";
        try {
            ChatResult result = next.proceed(context);
            outcome = "success";
            return result;
        } finally {
            log.info("event=model_call requestId={} outcome={} durationMs={}", context.requestId(),
                    outcome, (System.nanoTime() - started) / 1_000_000);
        }
    }
}
