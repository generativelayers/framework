package grl.body;

import grl.kernel.CandidateType;
import java.util.List;
import java.util.Map;

public record BodyDescriptor(
        String bodyId,
        BodyKind kind,
        String description,
        List<BodyAffordance> affordances,
        CandidateType defaultCandidateType,
        Map<String, String> metadata
) {
    public BodyDescriptor {
        bodyId = bodyId == null || bodyId.isBlank() ? "body.default" : bodyId;
        kind = kind == null ? BodyKind.CUSTOM : kind;
        description = description == null ? "" : description;
        affordances = affordances == null ? List.of() : List.copyOf(affordances);
        defaultCandidateType = defaultCandidateType == null ? CandidateType.CANDIDATE_ANSWER : defaultCandidateType;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
