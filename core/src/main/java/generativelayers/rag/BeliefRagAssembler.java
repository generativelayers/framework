package generativelayers.rag;

import generativelayers.kernel.BeliefSnapshot;
import java.util.List;

public final class BeliefRagAssembler {
    public String assemble(String instruction, BeliefSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append(instruction == null ? "" : instruction).append("\n\nApproved belief context:\n");
        List<String> beliefs = snapshot == null ? List.of() : snapshot.beliefTerms();
        for (String belief : beliefs) sb.append("- ").append(belief).append('\n');
        return sb.toString();
    }
}
