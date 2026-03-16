package sbuild.schematic;

import java.util.Map;

public final class SchematicParseException extends RuntimeException {
    private final String reasonCode;
    private final Map<String, Object> context;

    public SchematicParseException(String reasonCode, String message, Map<String, Object> context) {
        super(message);
        this.reasonCode = reasonCode;
        this.context = context == null ? Map.of() : Map.copyOf(context);
    }

    public SchematicParseException(String reasonCode, String message, Map<String, Object> context, Throwable cause) {
        super(message, cause);
        this.reasonCode = reasonCode;
        this.context = context == null ? Map.of() : Map.copyOf(context);
    }

    public String reasonCode() {
        return reasonCode;
    }

    public Map<String, Object> context() {
        return context;
    }
}
