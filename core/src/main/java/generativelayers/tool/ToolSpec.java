package generativelayers.tool;

import java.util.List;

public record ToolSpec(String name, String description, List<String> requiredArguments) {
    public ToolSpec {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        requiredArguments = requiredArguments == null ? List.of() : List.copyOf(requiredArguments);
    }
}
