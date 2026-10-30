package dev.learning.ai.launcher;

import dev.learning.ai.core.ModelRouter;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
final class ModelController {
    private final ModelRouter router;
    ModelController(ModelRouter router) { this.router = router; }

    /** Configuration discovery only; this does not probe upstream model health. */
    @GetMapping("/api/models")
    List<ModelRouter.Descriptor> models() { return router.models(); }
}
