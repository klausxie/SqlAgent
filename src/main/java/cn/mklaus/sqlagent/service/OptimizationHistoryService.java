package cn.mklaus.sqlagent.service;

import cn.mklaus.sqlagent.model.OptimizationHistory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing optimization history
 */
public class OptimizationHistoryService {
    private static final String HISTORY_DIR = System.getProperty("user.home") + "/.sqlagent";
    private static final String HISTORY_FILE = HISTORY_DIR + "/history.json";
    private static final int MAX_HISTORY_SIZE = 100;

    private final Gson gson;
    private List<OptimizationHistory> history;

    public OptimizationHistoryService() {
        this.gson = new Gson();
        this.history = loadHistory();
    }

    /**
     * Add optimization to history
     */
    public void addToHistory(String originalSql, String optimizedSql, String explanation) {
        String id = generateSqlHash(originalSql);
        double improvementScore = calculateImprovementScore(originalSql, optimizedSql);

        OptimizationHistory entry = new OptimizationHistory(
                id,
                originalSql,
                optimizedSql,
                explanation,
                improvementScore
        );

        // Check if entry already exists
        boolean exists = history.stream()
                .anyMatch(h -> h.getId().equals(id));

        if (!exists) {
            history.add(0, entry); // Add to beginning

            // Limit history size
            if (history.size() > MAX_HISTORY_SIZE) {
                history = history.subList(0, MAX_HISTORY_SIZE);
            }

            saveHistory();
        }
    }

    /**
     * Get all history entries
     */
    public List<OptimizationHistory> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Get history entry by ID
     */
    public OptimizationHistory getHistoryEntry(String id) {
        return history.stream()
                .filter(h -> h.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Mark optimization as applied
     */
    public void markAsApplied(String id) {
        OptimizationHistory entry = getHistoryEntry(id);
        if (entry != null) {
            entry.setApplied(true);
            saveHistory();
        }
    }

    /**
     * Clear all history
     */
    public void clearHistory() {
        history.clear();
        saveHistory();
    }

    /**
     * Generate hash for SQL (as unique ID)
     */
    private String generateSqlHash(String sql) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sql.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 8); // Use first 8 chars
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(sql.hashCode());
        }
    }

    /**
     * Calculate improvement score (simple heuristic)
     */
    private double calculateImprovementScore(String originalSql, String optimizedSql) {
        // Simple heuristic: shorter optimized SQL might be better
        int originalLength = originalSql.length();
        int optimizedLength = optimizedSql.length();

        if (originalLength == 0) return 0;

        double lengthImprovement = ((double) (originalLength - optimizedLength) / originalLength) * 50;

        // Check for specific improvements
        double featureImprovement = 0;
        if (optimizedSql.contains("INDEX")) featureImprovement += 20;
        if (optimizedSql.contains("JOIN") && !originalSql.contains("JOIN")) featureImprovement += 15;
        if (optimizedSql.contains("EXISTS") && !originalSql.contains("EXISTS")) featureImprovement += 15;

        return Math.min(100, Math.max(0, lengthImprovement + featureImprovement));
    }

    /**
     * Load history from file
     */
    private List<OptimizationHistory> loadHistory() {
        try {
            File file = new File(HISTORY_FILE);
            if (!file.exists()) {
                // Create directory and file
                file.getParentFile().mkdirs();
                file.createNewFile();
                return new ArrayList<>();
            }

            FileReader reader = new FileReader(file);
            Type listType = new TypeToken<ArrayList<OptimizationHistory>>() {}.getType();
            List<OptimizationHistory> loaded = gson.fromJson(reader, listType);
            reader.close();

            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {
            // Return empty list on error
            return new ArrayList<>();
        }
    }

    /**
     * Save history to file
     */
    private void saveHistory() {
        try {
            File file = new File(HISTORY_FILE);
            file.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(file);
            gson.toJson(history, writer);
            writer.close();
        } catch (IOException e) {
            // Log error but don't fail
            System.err.println("Failed to save history: " + e.getMessage());
        }
    }
}
