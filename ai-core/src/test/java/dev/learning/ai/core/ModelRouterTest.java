package dev.learning.ai.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ModelRouterTest {
    private ChatRequest request(String alias) {
        return new ChatRequest(alias, List.of(new Message("user", "hello")));
    }
    private ModelRouter.Route route(String alias, ChatModel model) {
        return new ModelRouter.Route(new ModelRouter.Descriptor(alias, "test", false), model);
    }
    private ChatResult result(String model) {
        return new ChatResult("test", model, "ok", ChatResult.Usage.unavailable(), false);
    }

    @Test void selectsExactlyOneRuleAndSnapshotsConfiguration() {
        List<String> calls = new ArrayList<>();
        List<ModelRouter.Route> routes = new ArrayList<>(List.of(
                route("fast", r -> { calls.add("small"); return result("small"); }),
                route("quality", r -> { calls.add("large"); return result("large"); })));
        ModelRouter router = new ModelRouter(routes);
        routes.clear();
        assertEquals("large", router.chat(request("quality")).model());
        assertEquals("small", router.chat(request("fast")).model());
        assertEquals(List.of("large", "small"), calls);
        assertThrows(UnsupportedOperationException.class, () -> router.models().clear());
    }

    @Test void unknownAliasNeverCallsProviderAndFailureNeverFallsBack() {
        AtomicInteger attempts = new AtomicInteger();
        ProviderException failure = new ProviderException();
        ModelRouter router = new ModelRouter(List.of(
                route("primary", r -> { attempts.incrementAndGet(); throw failure; }),
                route("backup", r -> { fail("unexpected fallback"); return null; })));
        assertThrows(UnknownModelException.class, () -> router.chat(request("unknown")));
        assertEquals(0, attempts.get());
        assertSame(failure, assertThrows(ProviderException.class, () -> router.chat(request("primary"))));
        assertEquals(1, attempts.get());
    }

    @Test void rejectsEmptyDuplicateAndUnsafeAliases() {
        assertThrows(IllegalArgumentException.class, () -> new ModelRouter(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ModelRouter(List.of(
                route("same", r -> result("a")), route("same", r -> result("b")))));
        for (String alias : new String[]{"", " ", "http://localhost", "a".repeat(81), "_unknown"})
            assertThrows(IllegalArgumentException.class, () -> route(alias, r -> result("a")));
    }
}
