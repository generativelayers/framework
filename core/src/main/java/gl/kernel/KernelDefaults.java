package gl.GovernanceKernel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KernelDefaults {
    private KernelDefaults() {}

    public static final class DeterministicFakeProvider implements KernelPorts.GenerativeProvider {
        public ProviderOutput generate(ResourceRequest request, Blob promptBlob) throws Exception {
            String mode = request.parameters().getOrDefault("mode", "valid");
            if ("fail".equalsIgnoreCase(mode)) throw new IllegalStateException("deterministic provider failure requested");
            String raw = switch (mode.toLowerCase()) {
                case "missing_field" -> "label=test\nsource=fake_provider";
                case "invalid" -> "this is not key value output";
                case "low_confidence" -> "label=test\nconfidence=0.25\nsource=fake_provider";
                case "conflict" -> "label=conflict\nconfidence=0.91\nsource=fake_provider\nwarning=conflicts_with_evidence";
                case "reflection" -> "verdict=needs_evidence\nconfidence=0.62\nsource=fake_provider\ncritique=insufficient grounding";
                case "tool_proposal" -> "tool=lookup\narguments=query:test\nconfidence=0.87\nsource=fake_provider";
                case "plan" -> "step1=retrieve_context\nstep2=validate_candidate\nstep3=ask_assessor\nconfidence=0.80\nsource=fake_provider";
                default -> "label=test\nconfidence=1.0\nsource=fake_provider";
            };
            return new ProviderOutput("fake", "deterministic-agentic-v0", raw, Map.of("mode", mode));
        }
    }

    public static final class KeyValueResponseValidator implements KernelPorts.ResponseValidator {
        public ValidationResult validate(String rawOutput, ResponseSchema schema) {
            Map<String, String> fields = parse(rawOutput);
            if (schema == null || schema.freeTextAllowed()) {
                if (fields.isEmpty()) return ValidationResult.valid(Map.of("text", rawOutput == null ? "" : rawOutput));
                return ValidationResult.valid(fields);
            }
            List<String> errors = new ArrayList<>();
            for (String required : schema.requiredFields()) {
                if (!fields.containsKey(required)) errors.add("missing required field: " + required);
            }
            if (!errors.isEmpty()) return ValidationResult.invalid("schema validation failed", errors);
            return ValidationResult.valid(fields);
        }

        private Map<String, String> parse(String raw) {
            Map<String, String> fields = new LinkedHashMap<>();
            if (raw == null || raw.isBlank()) return fields;
            for (String line : raw.split("\\R")) {
                int idx = line.indexOf('=');
                if (idx <= 0) continue;
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (!key.isBlank()) fields.put(key, value);
            }
            return fields;
        }
    }

    public static final class SimpleGovernancePolicy implements KernelPorts.GovernancePolicy {
        public PolicyDecision evaluate(ResourceRequest request) {
            if ("true".equalsIgnoreCase(request.parameters().getOrDefault("deny", "false"))) return new PolicyDecision(Outcomes.PolicyOutcome.DENY, "request denied by deterministic policy", Map.of());
            if ("true".equalsIgnoreCase(request.parameters().getOrDefault("escalate", "false"))) return new PolicyDecision(Outcomes.PolicyOutcome.ESCALATE, "request escalated by deterministic policy", Map.of());
            if (request.prompt().length() > 20000) return new PolicyDecision(Outcomes.PolicyOutcome.DENY, "prompt too large", Map.of("limit", "20000"));
            return new PolicyDecision(Outcomes.PolicyOutcome.ALLOW, "allowed", Map.of());
        }
    }

    public static final class SimpleAdmissibilityChecker implements KernelPorts.AdmissibilityChecker {
        public AdmissibilityDecision check(Candidate candidate, List<Assessment> assessments) {
            if (candidate == null) return new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.INADMISSIBLE, "missing candidate", Map.of());
            if (candidate.status() != CandidateStatus.VALIDATED && candidate.status() != CandidateStatus.ASSESSED && candidate.status() != CandidateStatus.ACCEPTED_BY_AGENT) return new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.INADMISSIBLE, "candidate is not validated or assessed", Map.of());
            boolean rejected = assessments != null && assessments.stream().anyMatch(a -> a.verdict() == Outcomes.AssessmentVerdict.REJECT && a.confidence() >= 0.5);
            if (rejected) return new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.INADMISSIBLE, "candidate has rejecting assessment", Map.of());
            return new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.ADMISSIBLE, "candidate is admissible", Map.of());
        }
    }
}
