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
     * Detect current platform
     * @return Platform identifier in format "os-arch" (e.g., "darwin-x86-64")
     */
    private String detectPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        String os;
        if (osName.contains("mac")) {
            os = "darwin";
        } else if (osName.contains("linux")) {
            os = "linux";
        } else if (osName.contains("win")) {
            os = "windows";
        } else {
            LOG.warn("Unknown OS: " + osName);
            return null;
        }

        // Normalize architecture names
        String normalizedArch;
        if (arch.equals("aarch64") || arch.equals("arm64")) {
            normalizedArch = "aarch64";
        } else if (arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64")) {
            normalizedArch = "x86-64";
        } else {
            LOG.warn("Unknown architecture: " + arch);
            return null;
        }

        return os + "-" + normalizedArch;
    }

    /**
     * Find OpenCode executable bundled with plugin
     * @return File if found, null otherwise
     */
    private File findBundledExecutable() {
        String platform = detectPlatform();
        if (platform == null) {
            LOG.debug("Could not detect platform for bundled executable");
            return null;
        }

        String executableName = platform.contains("windows") ? "opencode.exe" : "opencode";
        String resourcePath = "/bin/" + platform + "/" + executableName;

        try {
            // Get URL of bundled executable
            java.net.URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl == null) {
                LOG.debug("Bundled OpenCode not found at: " + resourcePath);
                return null;
            }

            // Convert URL to file path
            String filePath = java.net.URLDecoder.decode(resourceUrl.getPath(), "UTF-8");

            // Handle JAR URL format (jar:file:/path.jar!/bin/...)
            if (filePath.contains("!")) {
                // Extract to temp directory if in JAR
                return extractBundledExecutable(resourcePath, executableName);
            }

            File bundledFile = new File(filePath);
            if (bundledFile.exists() && bundledFile.canExecute()) {
                LOG.info("Found bundled OpenCode at: " + bundledFile.getAbsolutePath());
                return bundledFile;
            }

            // Make executable if needed
            if (!platform.contains("windows")) {
                bundledFile.setExecutable(true);
            }

            return bundledFile;

        } catch (Exception e) {
            LOG.warn("Failed to access bundled OpenCode: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract bundled executable from JAR to temp directory
     */
    private File extractBundledExecutable(String resourcePath, String executableName) {
        try {
            // Create temp directory for extracted binaries
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "sqlagent-opencode");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            File extractedFile = new File(tempDir, executableName);

            // Extract if not already extracted or version changed
            if (!extractedFile.exists()) {
                try (java.io.InputStream in = getClass().getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        return null;
                    }
                    java.nio.file.Files.copy(
                        in,
                        extractedFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                }

                // Set executable permission
                if (!resourcePath.contains("windows")) {
                    extractedFile.setExecutable(true);
                }

                LOG.info("Extracted bundled OpenCode to: " + extractedFile.getAbsolutePath());
            }

            return extractedFile;

        } catch (Exception e) {
            LOG.warn("Failed to extract bundled OpenCode: " + e.getMessage());
            return null;
        }
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

        // Priority 2: Bundled executable
        File bundledExecutable = findBundledExecutable();
        if (bundledExecutable != null) {
            LOG.info("Using bundled OpenCode executable");
            return bundledExecutable;
        }

        // Priority 3: Check PATH environment variable
        File pathExecutable = findInPath();
        if (pathExecutable != null) {
            LOG.info("Found OpenCode in PATH: " + pathExecutable.getAbsolutePath());
            return pathExecutable;
        }

        // Priority 4: Check common installation paths
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
