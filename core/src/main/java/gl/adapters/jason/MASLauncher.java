package gl.adapters.jason;

import jason.infra.local.RunLocalMAS;

import java.io.InputStream;
import java.util.logging.LogManager;

/**
 * Zero-boilerplate launcher for Jason MAS projects that use
 * the Generative Layers framework.
 *
 * <p>Handles all platform setup (logging, classpath, shutdown)
 * so that Jason developers only need a one-liner:
 *
 * <pre>
 *   public static void main(String[] args) throws Exception {
 *       MASLauncher.run("my_project.mas2j");
 *   }
 * </pre>
 *
 * <p>This class fixes the standalone Jason 3.x logging issue
 * where {@code jason-interpreter} references
 * {@code MASConsoleLogHandler} (only available in {@code jason-cli})
 * by applying a standard JUL configuration before booting the MAS.
 */
public final class MASLauncher {

    private MASLauncher() {}

    /**
     * Boot a Jason MAS from the given {@code .mas2j} project file.
     *
     * <p>The file must be on the classpath (e.g., in
     * {@code src/main/resources}).
     *
     * @param mas2jFile the project filename (e.g., {@code "my_project.mas2j"})
     * @throws Exception if the MAS fails to start
     */
    public static void run(String mas2jFile) throws Exception {
        configureLogging();
        RunLocalMAS.main(new String[]{ mas2jFile });
    }

    /**
     * Boot a Jason MAS, auto-detecting the {@code .mas2j} file
     * from the command-line args or falling back to a default.
     *
     * @param args           command-line args (first arg = .mas2j file)
     * @param defaultMas2j   fallback filename if no args are provided
     * @throws Exception if the MAS fails to start
     */
    public static void run(String[] args, String defaultMas2j) throws Exception {
        String mas2j = args.length > 0 ? args[0] : defaultMas2j;
        run(mas2j);
    }

    // ── Internal ────────────────────────────────────────────────

    private static void configureLogging() {
        try {
            // Extract our logging config to a temp file so we can set
            // java.util.logging.config.file BEFORE Jason's init() reads it
            InputStream config = MASLauncher.class
                    .getResourceAsStream("/gl/logging.properties");
            if (config != null) {
                java.io.File tmp = java.io.File.createTempFile("gl-logging", ".properties");
                tmp.deleteOnExit();
                try (java.io.OutputStream out = new java.io.FileOutputStream(tmp)) {
                    config.transferTo(out);
                }
                config.close();
                System.setProperty("java.util.logging.config.file", tmp.getAbsolutePath());
                LogManager.getLogManager().readConfiguration();
            }
        } catch (Exception e) {
            // Silently fall through — worst case is noisy logs
            System.err.println("[GL] Warning: could not configure logging: " + e.getMessage());
        }
    }
}
