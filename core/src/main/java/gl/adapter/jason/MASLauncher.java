package gl.adapter.jason;

import jason.infra.local.RunLocalMAS;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.LogManager;

/**
 * Zero-boilerplate launcher for Jason MAS projects that use
 * the Generative Layers framework.
 *
 * <p>Can be used directly as the main class — no per-project
 * {@code Launcher.java} is needed. The launcher auto-discovers
 * the {@code .mas2j} file on the classpath:
 *
 * <pre>
 *   # In pom.xml, just set mainClass to gl.adapter.jason.MASLauncher
 *   mvn compile exec:java
 * </pre>
 *
 * <p>Or pass the .mas2j filename as a command-line argument:
 * <pre>
 *   mvn compile exec:java -Dexec.args="my_project.mas2j"
 * </pre>
 *
 * <p>This class also fixes the standalone Jason 3.x logging issue
 * where {@code jason-interpreter} references
 * {@code MASConsoleLogHandler} (only available in {@code jason-cli})
 * by applying a standard JUL configuration before booting the MAS.
 */
public final class MASLauncher {

    // Runs before main() — prevents ClassNotFoundException for
    // jason.runtime.MASConsoleLogHandler (only available in jason-cli).
    static {
        try {
            InputStream config = MASLauncher.class
                    .getResourceAsStream("/gl/logging.properties");
            if (config != null) {
                LogManager.getLogManager().readConfiguration(config);
                config.close();
            }
        } catch (Exception ignored) {}
    }

    private MASLauncher() {}

    /**
     * Main entry point — auto-discovers or accepts a .mas2j argument.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Command-line argument (if provided)</li>
     *   <li>Auto-scan classpath for any {@code *.mas2j} file</li>
     * </ol>
     */
    public static void main(String[] args) throws Exception {
        String mas2j;
        if (args.length > 0) {
            mas2j = args[0];
        } else {
            mas2j = discoverMas2j();
            if (mas2j == null) {
                System.err.println("[GL] Error: No .mas2j file found on classpath and none specified as argument.");
                System.err.println("[GL] Usage: MASLauncher [my_project.mas2j]");
                System.exit(1);
                return;
            }
            System.out.println("[GL] Auto-discovered: " + mas2j);
        }
        run(mas2j);
    }

    /**
     * Boot a Jason MAS from the given {@code .mas2j} project file.
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

    /** Scan classpath root for any .mas2j file. Returns absolute path or null. */
    private static String discoverMas2j() {
        try {
            Enumeration<URL> roots = MASLauncher.class.getClassLoader().getResources("");
            while (roots.hasMoreElements()) {
                File dir = new File(roots.nextElement().toURI());
                if (dir.isDirectory()) {
                    for (File f : dir.listFiles()) {
                        if (f.isFile() && f.getName().endsWith(".mas2j")) {
                            return f.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fall through
        }
        return null;
    }

    private static void configureLogging() {
        try {
            InputStream config = MASLauncher.class
                    .getResourceAsStream("/gl/logging.properties");
            if (config != null) {
                File tmp = File.createTempFile("gl-logging", ".properties");
                tmp.deleteOnExit();
                try (java.io.OutputStream out = new java.io.FileOutputStream(tmp)) {
                    config.transferTo(out);
                }
                config.close();
                System.setProperty("java.util.logging.config.file", tmp.getAbsolutePath());
                LogManager.getLogManager().readConfiguration();
            }
        } catch (Exception e) {
            System.err.println("[GL] Warning: could not configure logging: " + e.getMessage());
        }
    }
}
