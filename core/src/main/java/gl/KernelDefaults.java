package gl;

import gl.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Default implementations of all {@link KernelPorts} interfaces.
 *  Includes: permit-all governance policy, key=value output validator,
 *  threshold-based admissibility checker, and a deterministic fake
 *  provider for offline testing. */
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

            String clean = raw.trim();
            // Strip markdown code fences if present (e.g. ```json ... ```)
            if (clean.startsWith("```")) {
                clean = clean.replaceAll("^```[a-zA-Z]*\\s*", "")
                             .replaceAll("```\\s*$", "")
                             .trim();
            }

            // Try parsing as structured JSON using a zero-dependency flat JSON parser.
            // Handles arrays, nested objects, and unquoted numeric/boolean values
            // by tracking brace/bracket nesting depth.
            if (clean.startsWith("{") && clean.endsWith("}")) {
                try {
                    String inner = clean.substring(1, clean.length() - 1).trim();
                    StringBuilder key = new StringBuilder();
                    StringBuilder val = new StringBuilder();
                    boolean inQuote = false;
                    boolean isKey = true;
                    boolean escaped = false;
                    int depth = 0; // nesting depth for [] and {}

                    for (int i = 0; i < inner.length(); i++) {
                        char c = inner.charAt(i);
                        if (escaped) {
                            char decoded = switch (c) {
                                case 'n' -> '\n';
                                case 'r' -> '\r';
                                case 't' -> '\t';
                                case 'b' -> '\b';
                                case 'f' -> '\f';
                                default -> c;
                            };
                            if (isKey) key.append(decoded); else val.append(decoded);
                            escaped = false;
                        } else if (c == '\\') {
                            escaped = true;
                        } else if (c == '"') {
                            inQuote = !inQuote;
                        } else if (!inQuote && (c == '[' || c == '{')) {
                            depth++;
                            if (!isKey) val.append(c);
                        } else if (!inQuote && (c == ']' || c == '}')) {
                            depth--;
                            if (!isKey) val.append(c);
                        } else if (c == ':' && !inQuote && depth == 0 && isKey) {
                            isKey = false;
                        } else if (c == ',' && !inQuote && depth == 0) {
                            String k = key.toString().trim();
                            String v = val.toString().trim();
                            if (!k.isEmpty()) {
                                fields.put(k, v);
                            }
                            key.setLength(0);
                            val.setLength(0);
                            isKey = true;
                        } else {
                            if (isKey) {
                                key.append(c);
                            } else {
                                val.append(c);
                            }
                        }
                    }
                    String k = key.toString().trim();
                    String v = val.toString().trim();
                    if (!k.isEmpty()) {
                        fields.put(k, v);
                    }
                    return fields;
                } catch (Exception e) {
                    System.err.println("[GL] Zero-dependency JSON parsing failed, falling back to key=value parser: " + e.getMessage());
                    fields.clear();
                }
            }

            // Fallback: Parse as traditional key=value lines
            for (String line : clean.split("\\R")) {
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
            if (candidate.status() != CandidateStatus.VALIDATED && candidate.status() != CandidateStatus.ASSESSED) return new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.INADMISSIBLE, "candidate is not validated or assessed", Map.of());
            boolean rejected = assessments != null && assessments.stream().anyMatch(a -> a.verdict() == Outcomes.AssessmentVerdict.REJECT_VERDICT && a.confidence() >= 0.5);
            if (rejected) return new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.INADMISSIBLE, "candidate has rejecting assessment", Map.of());
            return new AdmissibilityDecision(Outcomes.AdmissibilityOutcome.ADMISSIBLE, "candidate is admissible", Map.of());
        }
    }
}
