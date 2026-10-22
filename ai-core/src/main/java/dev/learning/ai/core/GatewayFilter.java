package dev.learning.ai.core;

@FunctionalInterface
public interface GatewayFilter {
    ChatResult apply(RequestContext context, Chain next);

    @FunctionalInterface
    interface Chain { ChatResult proceed(RequestContext context); }
}
