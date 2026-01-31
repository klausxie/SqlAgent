package cn.mklaus.sqlagent.ui;

import cn.mklaus.sqlagent.model.OptimizationResponse;
import cn.mklaus.sqlagent.model.OptimizationSuggestion;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for optimization tool window
 */
public class OptimizationPanel {
    private final Project project;
    private JPanel mainPanel;
    private JBTextArea logArea;
    private JBList<SuggestionItem> suggestionList;
    private DefaultListModel<SuggestionItem> suggestionModel;
    private JPanel metadataPanel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton applyButton;

    // Store optimization result for applying
    private Editor editor;
    private String originalSql;
    private String optimizedSql;
    private OptimizationResponse response;
    private Runnable applyCallback;

    private static final int MAX_LOG_LINES = 100; // Limit log lines to prevent EDT freeze

    public OptimizationPanel(Project project) {
        this.project = project;
        initUI();
    }

    private void initUI() {
        mainPanel = new JPanel(new BorderLayout(5, 5));

        // Top panel: Status and progress
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        topPanel.add(statusLabel, BorderLayout.NORTH);

        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        topPanel.add(progressBar, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center panel: Split log and metadata
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerSplit.setResizeWeight(0.6);

        // Log area
        logArea = new JBTextArea();
        logArea.setEditable(false);
        logArea.setBackground(UIUtil.getPanelBackground());
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JBScrollPane logScroll = new JBScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Optimization Log"));
        centerSplit.setTopComponent(logScroll);

        // Metadata panel
        metadataPanel = new JPanel(new GridLayout(0, 1));
        metadataPanel.setBorder(BorderFactory.createTitledBorder("Table Metadata"));
        JBScrollPane metadataScroll = new JBScrollPane(metadataPanel);
        centerSplit.setBottomComponent(metadataScroll);

        mainPanel.add(centerSplit, BorderLayout.CENTER);

        // Bottom panel: Suggestions list and Apply button
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

        suggestionModel = new DefaultListModel<>();
        suggestionList = new JBList<>(suggestionModel);
        suggestionList.setCellRenderer(new SuggestionListRenderer());
        suggestionList.setEmptyText("No optimization suggestions yet");

        JBScrollPane listScroll = new JBScrollPane(suggestionList);
        listScroll.setBorder(BorderFactory.createTitledBorder("Optimization Suggestions"));
        listScroll.setPreferredSize(new Dimension(-1, 150));

        bottomPanel.add(listScroll, BorderLayout.CENTER);

        // Apply button
        applyButton = new JButton("Apply Optimized SQL");
        applyButton.setEnabled(false);
        applyButton.addActionListener(e -> {
            if (applyCallback != null) {
                applyCallback.run();
            }
        });
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(applyButton, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        log("SQL Agent tool window ready");
        log("Right-click on SQL in editor and select 'Optimize SQL with AI'");
    }

    public JPanel getPanel() {
        return mainPanel;
    }

    public void log(String message) {
        runOnEdt(() -> {
            java.time.LocalTime time = java.time.LocalTime.now();
            String logMessage = String.format("[%s] %s", time, message);
            logArea.append(logMessage + "\n");
            limitLogLines();
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Run action on Event Dispatch Thread
     */
    private void runOnEdt(Runnable action) {
        ApplicationManager.getApplication().invokeLater(action);
    }

    /**
     * Limit log lines to prevent EDT freeze
     */
    private void limitLogLines() {
        String text = logArea.getText();
        String[] lines = text.split("\n", -1);
        if (lines.length > MAX_LOG_LINES) {
            StringBuilder newText = new StringBuilder();
            for (int i = lines.length - MAX_LOG_LINES; i < lines.length; i++) {
                newText.append(lines[i]).append("\n");
            }
            logArea.setText(newText.toString());
        }
    }

    public void clear() {
        runOnEdt(() -> {
            logArea.setText("");
            suggestionModel.clear();
            clearMetadata();
        });
    }

    public void setStatus(String status, boolean busy) {
        runOnEdt(() -> {
            statusLabel.setText(status);
            progressBar.setVisible(busy);
            if (busy) {
                progressBar.setIndeterminate(true);
            }
            mainPanel.revalidate();
        });
    }

    public void setProgress(int progress) {
        runOnEdt(() -> {
            progressBar.setIndeterminate(false);
            progressBar.setValue(progress);
        });
    }

    public void addMetadata(String key, String value) {
        runOnEdt(() -> {
            JPanel panel = new JPanel(new BorderLayout(5, 0));
            JLabel keyLabel = new JLabel(key + ":");
            keyLabel.setFont(keyLabel.getFont().deriveFont(Font.BOLD));
            JLabel valueLabel = new JLabel(value);
            panel.add(keyLabel, BorderLayout.WEST);
            panel.add(valueLabel, BorderLayout.CENTER);
            metadataPanel.add(panel);
            metadataPanel.revalidate();
        });
    }

    public void clearMetadata() {
        runOnEdt(() -> {
            metadataPanel.removeAll();
            metadataPanel.revalidate();
            metadataPanel.repaint();
        });
    }

    public void addSuggestion(OptimizationResponse response) {
        runOnEdt(() -> {
            if (response.getSuggestions() != null && !response.getSuggestions().isEmpty()) {
                for (OptimizationSuggestion suggestion : response.getSuggestions()) {
                    suggestionModel.addElement(new SuggestionItem(suggestion));
                }
            }
        });
    }

    public void clearSuggestions() {
        runOnEdt(() -> {
            suggestionModel.clear();
        });
    }

    /**
     * Set the optimization result and enable the apply button
     */
    public void setOptimizationResult(Editor editor, String originalSql, String optimizedSql,
                                       OptimizationResponse response, Runnable applyCallback) {
        this.editor = editor;
        this.originalSql = originalSql;
        this.optimizedSql = optimizedSql;
        this.response = response;
        this.applyCallback = applyCallback;

        runOnEdt(() -> {
            applyButton.setEnabled(true);
            updateApplyButtonText();
        });
    }

    /**
     * Clear the optimization result and disable the apply button
     */
    public void clearOptimizationResult() {
        this.editor = null;
        this.originalSql = null;
        this.optimizedSql = null;
        this.response = null;
        this.applyCallback = null;

        runOnEdt(() -> {
            applyButton.setEnabled(false);
            applyButton.setText("Apply Optimized SQL");
        });
    }

    /**
     * Update apply button text with improvement percentage
     */
    private void updateApplyButtonText() {
        if (response != null && response.getEstimatedImprovement() > 0) {
            double improvement = response.getEstimatedImprovement() * 100;
            applyButton.setText(String.format("Apply Optimized SQL (%.1f%% improvement)", improvement));
        } else {
            applyButton.setText("Apply Optimized SQL");
        }
    }

    /**
     * Custom list item for suggestions
     */
    public static class SuggestionItem {
        private final OptimizationSuggestion suggestion;

        public SuggestionItem(OptimizationSuggestion suggestion) {
            this.suggestion = suggestion;
        }

        public OptimizationSuggestion getSuggestion() {
            return suggestion;
        }

        @Override
        public String toString() {
            return suggestion.getTitle();
        }
    }

    /**
     * Custom cell renderer for suggestions
     */
    private static class SuggestionListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (c instanceof JLabel && value instanceof SuggestionItem) {
                SuggestionItem item = (SuggestionItem) value;
                OptimizationSuggestion s = item.getSuggestion();
                setText("<html><b>" + escapeHtml(s.getTitle()) + "</b><br>" +
                        "<small>" + escapeHtml(s.getDescription()) + "</small></html>");
            }
            return c;
        }

        private String escapeHtml(String text) {
            if (text == null) return "";
            return text.replace("<", "&lt;").replace(">", "&gt;");
        }
    }
}
