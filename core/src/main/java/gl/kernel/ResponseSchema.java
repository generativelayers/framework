package gl.GovernanceKernel;

import java.util.List;

public record ResponseSchema(String name, List<String> requiredFields, boolean freeTextAllowed) {
    public ResponseSchema {
        name = name == null || name.isBlank() ? "free_text" : name;
        requiredFields = requiredFields == null ? List.of() : List.copyOf(requiredFields);
    }
    public static ResponseSchema freeText() { return new ResponseSchema("free_text", List.of(), true); }
    public static ResponseSchema required(String name, List<String> fields) { return new ResponseSchema(name, fields, false); }
}
