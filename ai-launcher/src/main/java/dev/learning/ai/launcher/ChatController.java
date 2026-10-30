package dev.learning.ai.launcher;

import dev.learning.ai.core.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
final class ChatController {
    private final GatewayPipeline pipeline;
    ChatController(GatewayPipeline pipeline) { this.pipeline = pipeline; }

    @PostMapping("/api/chat")
    ChatResponse chat(@Valid @RequestBody ChatInput input, HttpServletRequest http) {
        ChatRequest request = new ChatRequest(input.model(), input.messages().stream()
                .map(m -> new Message(m.role(), m.content())).toList());
        String id = (String) http.getAttribute(RequestIdFilter.ATTRIBUTE);
        ChatResult result = pipeline.execute(new RequestContext(id, request));
        return new ChatResponse(id, request.model(), result.provider(), result.model(),
                result.content(), result.usage(), result.simulated());
    }

    record ChatInput(@NotBlank @Size(max=80) String model,
                     @NotEmpty @Size(max=32) List<@NotNull @Valid InputMessage> messages) {}
    record InputMessage(@NotBlank String role, @NotBlank @Size(max=16000) String content) {}
    record ChatResponse(String requestId, String model, String provider, String upstreamModel,
                        String content, ChatResult.Usage usage, boolean simulated) {}
}
