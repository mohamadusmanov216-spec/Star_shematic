package sbuild.ai;

import org.junit.jupiter.api.Test;
import sbuild.state.BuildStateService;
import sbuild.storage.StorageService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiServiceTest {
    @Test
    void routesResourceQuestionToMaterialsIntent() {
        AiService service = new AiService();
        AiService.AssistantReply reply = service.respond("Какие ресурсы нужны?", new BuildStateService(), new StorageService());

        assertEquals("command.sbuild.ai.reply.materials.no_schematic", reply.key());
    }
}
