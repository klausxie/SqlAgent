package cn.mklaus.sqlagent.ui;

import cn.mklaus.sqlagent.model.OptimizationHistory;
import cn.mklaus.sqlagent.service.OptimizationHistoryService;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel for displaying optimization history
 */
public class HistoryPanel {
    private final JPanel mainPanel;
    private final OptimizationHistoryService historyService;
    private JList<HistoryItem> historyList;
    private DefaultListModel<HistoryItem> historyModel;
    private JTextArea detailArea;
    private JButton refreshButton;
    private JButton clearButton;
    private JButton compareButton;

    public HistoryPanel() {
        this.historyService = new OptimizationHistoryService();
        this.mainPanel = createPanel();
        loadHistory();
    }

    private JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title and button panel
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Optimization History");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        refreshButton = new JButton("🔄 Refresh");
        clearButton = new JButton("🗑️ Clear");
        compareButton = new JButton("⚖️ Compare");

        buttonPanel.add(refreshButton);
        buttonPanel.add(compareButton);
        buttonPanel.add(clearButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        // Split pane: list and details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.4);

        // History list
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setCellRenderer(new HistoryListRenderer());
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showHistoryDetails();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(historyList);
        listScroll.setBorder(BorderFactory.createTitledBorder("History"));
        splitPane.setLeftComponent(listScroll);

        // Detail area
        detailArea = new JTextArea();
        detailArea.setEditable(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createTitledBorder("Details"));
        splitPane.setRightComponent(detailScroll);

        panel.add(splitPane, BorderLayout.CENTER);

        // Button actions
        refreshButton.addActionListener(this::refreshHistory);
        clearButton.addActionListener(this::clearHistory);
        compareButton.addActionListener(this::compareSelected);

        return panel;
    }

    /**
     * Load history from service
     */
    public void loadHistory() {
        List<OptimizationHistory> history = historyService.getHistory();
        historyModel.clear();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");

        for (OptimizationHistory entry : history) {
            String timestamp = entry.getTimestamp().format(formatter);
            String preview = getPreview(entry.getOriginalSql());
            double score = entry.getImprovementScore();

            historyModel.addElement(new HistoryItem(entry, timestamp, preview, score));
        }
    }

    /**
     * Show details of selected history entry
     */
    private void showHistoryDetails() {
        HistoryItem selected = historyList.getSelectedValue();
        if (selected == null) {
            detailArea.setText("Select an entry to view details");
            return;
        }

        OptimizationHistory entry = selected.getEntry();

        StringBuilder details = new StringBuilder();
        details.append("TIMESTAMP: ").append(entry.getTimestamp()).append("\n");
        details.append("IMPROVEMENT SCORE: ").append(String.format("%.1f", entry.getImprovementScore())).append("/100\n");
        details.append("APPLIED: ").append(entry.isApplied() ? "Yes" : "No").append("\n");
        details.append("\n────────────────────────────────────────\n");
        details.append("ORIGINAL SQL:\n");
        details.append(entry.getOriginalSql()).append("\n");
        details.append("\n────────────────────────────────────────\n");
        details.append("OPTIMIZED SQL:\n");
        details.append(entry.getOptimizedSql()).append("\n");
        details.append("\n────────────────────────────────────────\n");
        details.append("EXPLANATION:\n");
        details.append(entry.getExplanation()).append("\n");

        detailArea.setText(details.toString());
    }

    /**
     * Get preview of SQL (first 50 chars)
     */
    private String getPreview(String sql) {
        if (sql == null) return "";
        String preview = sql.trim().replaceAll("\\s+", " ");
        return preview.length() > 50 ? preview.substring(0, 47) + "..." : preview;
    }

    /**
     * Refresh history
     */
    private void refreshHistory(ActionEvent e) {
        loadHistory();
    }

    /**
     * Clear all history
     */
    private void clearHistory(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(
                mainPanel,
                "Are you sure you want to clear all optimization history?",
                "Clear History",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            historyService.clearHistory();
            loadHistory();
            detailArea.setText("");
        }
    }

    /**
     * Compare selected entries
     */
    private void compareSelected(ActionEvent e) {
        HistoryItem selected = historyList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(
                    mainPanel,
                    "Please select an entry to compare",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        OptimizationHistory entry = selected.getEntry();

        // Show diff dialog
        DiffViewer.showDiffInEditor(
                null,  // No project needed for history view
                entry.getOriginalSql(),
                entry.getOptimizedSql(),
                null   // No response needed
        );
    }

    /**
     * Get main panel
     */
    public JPanel getPanel() {
        return mainPanel;
    }

    /**
     * History item for list display
     */
    private static class HistoryItem {
        private final OptimizationHistory entry;
        private final String timestamp;
        private final String preview;
        private final double score;

        public HistoryItem(OptimizationHistory entry, String timestamp, String preview, double score) {
            this.entry = entry;
            this.timestamp = timestamp;
            this.preview = preview;
            this.score = score;
        }

        public OptimizationHistory getEntry() {
            return entry;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getPreview() {
            return preview;
        }

        public double getScore() {
            return score;
        }
    }

    /**
     * List cell renderer for history items
     */
    private static class HistoryListRenderer extends JPanel implements ListCellRenderer<HistoryItem> {
        private final JLabel timestampLabel;
        private final JLabel previewLabel;
        private final JLabel scoreLabel;

        public HistoryListRenderer() {
            setLayout(new BorderLayout(5, 5));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            setOpaque(true);

            JPanel leftPanel = new JPanel(new BorderLayout());
            timestampLabel = new JLabel();
            timestampLabel.setFont(new Font("Arial", Font.BOLD, 11));
            leftPanel.add(timestampLabel, BorderLayout.NORTH);

            previewLabel = new JLabel();
            previewLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
            leftPanel.add(previewLabel, BorderLayout.CENTER);

            add(leftPanel, BorderLayout.CENTER);

            scoreLabel = new JLabel();
            scoreLabel.setFont(new Font("Arial", Font.BOLD, 12));
            scoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            add(scoreLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends HistoryItem> list,
                                                      HistoryItem value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            if (value != null) {
                timestampLabel.setText(value.getTimestamp());
                previewLabel.setText(value.getPreview());

                double score = value.getScore();
                scoreLabel.setText(String.format("%.0f", score));

                // Color code score
                if (score >= 70) {
                    scoreLabel.setForeground(new Color(0, 150, 0));
                } else if (score >= 40) {
                    scoreLabel.setForeground(new Color(200, 150, 0));
                } else {
                    scoreLabel.setForeground(new Color(200, 50, 50));
                }
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }
}
