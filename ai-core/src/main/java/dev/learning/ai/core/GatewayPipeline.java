package dev.learning.ai.core;

import java.util.List;
import java.util.Objects;

/** Immutable composition: each request gets its own call stack, with no shared cursor. */
public final class GatewayPipeline {
    private final GatewayFilter.Chain chain;

    public GatewayPipeline(List<GatewayFilter> filters, ChatModel terminal) {
        Objects.requireNonNull(terminal);
        GatewayFilter.Chain composed = ctx -> terminal.chat(ctx.request());
        List<GatewayFilter> snapshot = List.copyOf(filters);
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            GatewayFilter filter = snapshot.get(i);
            GatewayFilter.Chain next = composed;
            composed = ctx -> filter.apply(ctx, next);
        }
        chain = composed;
    }

    public ChatResult execute(RequestContext context) { return chain.proceed(context); }
}
