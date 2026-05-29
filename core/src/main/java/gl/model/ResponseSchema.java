package gl.model;

import java.util.List;

/** Expected output schema for a generation request.
 *  Defines required field names that the output validator will check.
 *  Use {@link #freeText()} for unstructured output or
 *  {@link #required(String, java.util.List)} for key=value validation. */
public record ResponseSchema(String name, List<String> requiredFields, boolean freeTextAllowed) {
    public ResponseSchema {
        name = name == null || name.isBlank() ? "free_text" : name;
        requiredFields = requiredFields == null ? List.of() : List.copyOf(requiredFields);
    }
    public static ResponseSchema freeText() { return new ResponseSchema("free_text", List.of(), true); }
    public static ResponseSchema required(String name, List<String> fields) { return new ResponseSchema(name, fields, false); }
}
