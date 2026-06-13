package gl.provider;

/**
 * Shared utilities for provider implementations.
 *
 * <p>Extracts common JSON string parsing and escaping logic
 * used by both {@link GeminiProvider} and {@link ChatCompletionsProvider}.
 */
final class ProviderUtils {
    private ProviderUtils() {}

    /**
     * Extract the first JSON string value following the given marker.
     *
     * <p>Handles escape sequences ({@code \n}, {@code \r}, {@code \t}, {@code \\}, {@code \"}).
     * Scans for the marker, finds the opening quote, and reads until the closing quote.
     *
     * @param json   the raw JSON response body
     * @param marker the field name to search for (e.g. {@code "\"text\":"} or {@code "\"content\":"})
     * @return the extracted string value, or {@code null} if not found
     */
    static String extractJsonString(String json, String marker) {
        int idx = json.indexOf(marker);
        if (idx < 0) return null;
        int start = json.indexOf('"', idx + marker.length());
        if (start < 0) return null;
        StringBuilder out = new StringBuilder();
        boolean esc = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) {
                out.append(switch (c) {
                    case 'n' -> '\n'; case 'r' -> '\r';
                    case 't' -> '\t'; default -> c;
                });
                esc = false;
            } else if (c == '\\') { esc = true; }
            else if (c == '"') { break; }
            else { out.append(c); }
        }
        return out.toString();
    }

    /**
     * Escape a string for embedding in a JSON string literal.
     *
     * @param text the raw text to escape
     * @return the escaped text (safe for JSON string embedding)
     */
    static String escape(String text) {
        return (text == null ? "" : text)
                .replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r")
                .replace("\t", "\\t").replace("\b", "\\b")
                .replace("\f", "\\f");
    }
}
