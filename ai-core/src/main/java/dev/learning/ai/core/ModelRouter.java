package dev.learning.ai.core;

import java.util.*;

/** Exact alias rules, captured once at startup. No default route for unknown input. */
public final class ModelRouter implements ChatModel {
    public record Descriptor(String alias, String provider, boolean simulated) {}
    public record Route(Descriptor descriptor, ChatModel target) {
        public Route {
            Objects.requireNonNull(descriptor);
            Objects.requireNonNull(target);
            if (descriptor.alias() == null || !descriptor.alias().matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,79}"))
                throw new IllegalArgumentException("Invalid route alias");
            if (descriptor.provider() == null || descriptor.provider().isBlank())
                throw new IllegalArgumentException("Invalid route provider");
        }
    }

    private final Map<String, Route> routes;
    private final List<Descriptor> models;

    public ModelRouter(List<Route> routes) {
        if (routes.isEmpty()) throw new IllegalArgumentException("At least one route is required");
        Map<String, Route> snapshot = new LinkedHashMap<>();
        for (Route route : routes) {
            if (snapshot.putIfAbsent(route.descriptor().alias(), route) != null)
                throw new IllegalArgumentException("Duplicate route alias");
        }
        this.routes = Map.copyOf(snapshot);
        this.models = snapshot.values().stream().map(Route::descriptor).toList();
    }

    public List<Descriptor> models() { return models; }

    public Optional<Descriptor> describe(String alias) {
        Route route = routes.get(alias);
        return route == null ? Optional.empty() : Optional.of(route.descriptor());
    }

    @Override public ChatResult chat(ChatRequest request) {
        Route route = routes.get(request.model());
        if (route == null) throw new UnknownModelException();
        return route.target().chat(request);
    }
}
