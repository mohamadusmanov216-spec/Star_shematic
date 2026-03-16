package sbuild.ai;

import sbuild.state.BuildStateService;
import sbuild.storage.StorageService;

import java.util.Locale;

/**
 * Local assistant layer for /ai_help command.
 * Provides useful contextual hints without simulating external LLM behavior.
 */
public final class AiService {
    public String respond(String rawQuery, BuildStateService state, StorageService storageService) {
        String query = normalize(rawQuery);
        if (query.isBlank()) {
            return "Сформулируй запрос: например, 'как загрузить схему' или 'что делать дальше'.";
        }

        if (containsAny(query, "загруз", "load", "схем")) {
            return "Используй /sbuild schematic list, затем /sbuild schematic load <name>.";
        }
        if (containsAny(query, "что сейчас загруж", "current schematic", "loaded")) {
            return state.loadedSchematic()
                .map(s -> "Сейчас загружена схема " + s.name() + ", блоков: " + s.blockCount() + ".")
                .orElse("Схема не загружена. Начни с /sbuild schematic list и /sbuild schematic load <name>.");
        }
        if (containsAny(query, "дальше", "next", "что делать")) {
            return state.loadedSchematic().isPresent()
                ? "Следующий шаг: проверь /sbuild materials report или /sbuild planner preview."
                : "Сначала загрузи схему: /sbuild schematic list -> /sbuild schematic load <name>.";
        }
        if (containsAny(query, "storage", "сундук", "склад", "chest")) {
            int points = storageService.listStoragePoints().size();
            return "Хранилища: " + points + ". Добавь точку: /sbuild chest set <name>, список: /sbuild chest list.";
        }
        if (containsAny(query, "materials", "материал")) {
            return "Для сметы используй /sbuild materials report. Команда покажет required / built / remaining / available.";
        }
        if (containsAny(query, "planner", "план", "preview")) {
            return "Проверь план укладки через /sbuild planner preview после загрузки схемы.";
        }
        if (containsAny(query, "help", "помощ")) {
            return "Я умею: подсказать команды, объяснить статус схемы, storage/materials/planner и следующий шаг.";
        }

        return "Пока умею помогать по schematic/storage/materials/planner и шагам сборки. Попробуй: 'как загрузить схему'.";
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
}
