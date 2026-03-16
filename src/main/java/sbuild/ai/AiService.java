package sbuild.ai;

import sbuild.state.BuildStateService;
import sbuild.storage.StorageService;

import java.util.Locale;

/**
 * Local assistant layer for /ai_help command.
 * Provides contextual hints without pretending to be a remote AI model.
 */
public final class AiService {
    public AssistantReply respond(String rawQuery, BuildStateService state, StorageService storageService) {
        String query = normalize(rawQuery);
        if (query.isBlank()) {
            return reply("command.sbuild.ai.reply.empty_query");
        }

        boolean hasLoadedSchematic = state.loadedSchematic().isPresent();
        int storageCount = storageService.listStoragePoints().size();

        if (containsAny(query,
            "status", "статус", "progress", "прогресс", "state", "состояние", "what now", "что сейчас")) {
            if (hasLoadedSchematic) {
                var loaded = state.loadedSchematic().orElseThrow();
                String name = loaded.name();
                int blocks = loaded.blockCount();
                return reply("command.sbuild.ai.reply.status.loaded", name, blocks, storageCount);
            }
            return reply("command.sbuild.ai.reply.status.no_schematic", storageCount);
        }

        if (containsAny(query,
            "load", "loaded", "загруз", "подгруз", "откры", "схем", "litematic", "schematic")) {
            return reply("command.sbuild.ai.reply.load_howto");
        }

        if (containsAny(query,
            "next", "дальше", "what to do", "что делать", "следующий", "step", "шаг")) {
            if (!hasLoadedSchematic) {
                return reply("command.sbuild.ai.reply.next_step.no_schematic");
            }
            if (storageCount == 0) {
                return reply("command.sbuild.ai.reply.next_step.no_storage");
            }
            return reply("command.sbuild.ai.reply.next_step.ready");
        }

        if (containsAny(query,
            "storage", "chest", "сундук", "склад", "хранил", "restock", "ресурс")) {
            return storageCount == 0
                ? reply("command.sbuild.ai.reply.storage.none")
                : reply("command.sbuild.ai.reply.storage.with_count", storageCount);
        }

        if (containsAny(query,
            "material", "материал", "ресурс", "need", "нужно", "хват", "enough")) {
            return hasLoadedSchematic
                ? reply("command.sbuild.ai.reply.materials.ready")
                : reply("command.sbuild.ai.reply.materials.no_schematic");
        }

        if (containsAny(query,
            "planner", "plan", "план", "preview", "очеред", "order")) {
            return hasLoadedSchematic
                ? reply("command.sbuild.ai.reply.planner.ready")
                : reply("command.sbuild.ai.reply.planner.no_schematic");
        }

        if (containsAny(query,
            "help", "помощ", "команды", "commands", "что умеешь", "what can")) {
            return reply("command.sbuild.ai.reply.help");
        }

        return hasLoadedSchematic
            ? reply("command.sbuild.ai.reply.unknown.loaded", storageCount)
            : reply("command.sbuild.ai.reply.unknown.no_schematic", storageCount);
    }

    private AssistantReply reply(String key, Object... args) {
        return new AssistantReply(key, args);
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public record AssistantReply(String key, Object[] args) {
    }
}
