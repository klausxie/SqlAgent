package cn.mklaus.sqlagent.opencode;

import com.intellij.openapi.diagnostic.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Locates OpenCode executable on the system
 */
public class OpenCodeLocator {
    private static final Logger LOG = Logger.getInstance(OpenCodeLocator.class);

    private final String customExecutablePath;

    public OpenCodeLocator(String customExecutablePath) {
        this.customExecutablePath = customExecutablePath;
    }

    /**
     * Find OpenCode executable file
     * @return File if found, null otherwise
     */
    public File findOpenCodeExecutable() {
        // Priority 1: User custom path from settings
        if (customExecutablePath != null && !customExecutablePath.trim().isEmpty()) {
            File customFile = new File(customExecutablePath);
            if (customFile.exists() && customFile.canExecute()) {
                LOG.info("Found OpenCode at custom path: " + customExecutablePath);
                return customFile;
            }
            LOG.warn("Custom OpenCode path specified but not executable: " + customExecutablePath);
        }

        // Priority 2: Check PATH environment variable
        File pathExecutable = findInPath();
        if (pathExecutable != null) {
            LOG.info("Found OpenCode in PATH: " + pathExecutable.getAbsolutePath());
            return pathExecutable;
        }

        // Priority 3: Check common installation paths
        File commonPathExecutable = findInCommonPaths();
        if (commonPathExecutable != null) {
            LOG.info("Found OpenCode in common path: " + commonPathExecutable.getAbsolutePath());
            return commonPathExecutable;
        }

        LOG.warn("OpenCode executable not found");
        return null;
    }

    /**
     * Search in PATH environment variable
     */
    private File findInPath() {
        String osName = System.getProperty("os.name").toLowerCase();

        try {
            ProcessBuilder pb;
            if (osName.contains("win")) {
                pb = new ProcessBuilder("where", "opencode");
            } else {
                pb = new ProcessBuilder("which", "opencode");
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();

            String firstLine = output.toString().trim().split("\n")[0];
            if (firstLine != null && !firstLine.isEmpty()) {
                File executable = new File(firstLine);
                if (executable.exists() && executable.isFile()) {
                    return executable;
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to search PATH: " + e.getMessage());
        }

        return null;
    }

    /**
     * Search in common installation paths
     */
    private File findInCommonPaths() {
        String osName = System.getProperty("os.name").toLowerCase();
        List<String> paths = new ArrayList<>();

        if (osName.contains("mac")) {
            paths.add("/usr/local/bin/opencode");
            paths.add("/opt/homebrew/bin/opencode");
            paths.add(System.getProperty("user.home") + "/bin/opencode");
        } else if (osName.contains("linux")) {
            paths.add("/usr/local/bin/opencode");
            paths.add("/usr/bin/opencode");
            paths.add(System.getProperty("user.home") + "/.local/bin/opencode");
            paths.add(System.getProperty("user.home") + "/bin/opencode");
        } else if (osName.contains("win")) {
            paths.add("C:\\Program Files\\OpenCode\\opencode.exe");
            paths.add("C:\\Program Files (x86)\\OpenCode\\opencode.exe");
            String userprofile = System.getenv("USERPROFILE");
            if (userprofile != null) {
                paths.add(userprofile + "\\AppData\\Local\\OpenCode\\opencode.exe");
            }
        }

        for (String path : paths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                return file;
            }
        }

        return null;
    }

    /**
     * Get OpenCode version
     * @return version string if successful, null otherwise
     */
    public String getOpenCodeVersion() {
        File executable = findOpenCodeExecutable();
        if (executable == null) {
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath(), "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();

            if (line != null) {
                return line.trim();
            }
        } catch (Exception e) {
            LOG.warn("Failed to get OpenCode version: " + e.getMessage());
        }

        return null;
    }
}
