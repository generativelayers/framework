package gl.model;

import java.util.List;
import java.util.Map;

/** Result of validating a provider's raw output against a {@link ResponseSchema}.
 *  Contains extracted key=value fields, validation errors, and a summary message. */
public record ValidationResult(boolean valid, String message, Map<String, String> fields, List<String> errors) {
    public ValidationResult {
        message = message == null ? "" : message;
        fields = fields == null ? Map.of() : Map.copyOf(fields);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
    public static ValidationResult valid(Map<String, String> fields) { return new ValidationResult(true, "valid", fields, List.of()); }
    public static ValidationResult invalid(String message, List<String> errors) { return new ValidationResult(false, message, Map.of(), errors); }
}
