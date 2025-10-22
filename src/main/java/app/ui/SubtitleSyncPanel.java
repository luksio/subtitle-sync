package app.ui;

import app.model.FrameRate;
import app.service.SubtitleService;
import app.service.VideoMetadataService;
import lombok.extern.java.Log;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class SubtitleSyncPanel extends JPanel {
    private static final int SLIDER_MIN = -60;
    private static final int SLIDER_MAX = 60;
    private static final int SLIDER_MAJOR_TICK_SPACING = 20;
    private static final int SLIDER_MINOR_TICK_SPACING = 5;

    private final SubtitleService subtitleService;
    private final VideoMetadataService videoMetadataService;
    private File currentSubtitleFile;

    // UI Components
    private JLabel subtitleLabel;
    private JLabel offsetValueLabel;
    private JSlider offsetSlider;
    private JButton subtitleButton;
    private JButton saveButton;
    private JButton saveFrameRateButton;

    // Frame rate components
    private JComboBox<FrameRate> fromFrameRateCombo;
    private JComboBox<FrameRate> toFrameRateCombo;
    private JButton detectFromVideoButton;
    private JTabbedPane tabbedPane;

    public SubtitleSyncPanel() {
        this.subtitleService = new SubtitleService();
        this.videoMetadataService = new VideoMetadataService();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeComponents() {
        subtitleLabel = new JLabel("No subtitle file selected");
        offsetValueLabel = new JLabel("0.0 s");

        subtitleButton = new JButton("Select SRT File");
        saveButton = new JButton("Save Shifted Subtitles");
        saveFrameRateButton = new JButton("Save Converted Subtitles");

        // Button with text wrapping in HTML
        detectFromVideoButton = new JButton("<html><center>📹 Detect from<br>video</center></html>");

        offsetSlider = new JSlider(JSlider.HORIZONTAL, SLIDER_MIN, SLIDER_MAX, 0);
        offsetSlider.setMajorTickSpacing(SLIDER_MAJOR_TICK_SPACING);
        offsetSlider.setMinorTickSpacing(SLIDER_MINOR_TICK_SPACING);
        offsetSlider.setPaintTicks(true);
        offsetSlider.setPaintLabels(true);

        // Frame rate components with limited width
        fromFrameRateCombo = new JComboBox<>(FrameRate.values());
        toFrameRateCombo = new JComboBox<>(FrameRate.values());
        FrameRate defaultFrameRate = FrameRate.values()[0];
        fromFrameRateCombo.setSelectedItem(defaultFrameRate);
        toFrameRateCombo.setSelectedItem(defaultFrameRate);

        // Width limitation for BOTH combo boxes
        Dimension comboSize = new Dimension(300, fromFrameRateCombo.getPreferredSize().height);
        fromFrameRateCombo.setPreferredSize(comboSize);
        fromFrameRateCombo.setMaximumSize(comboSize);
        toFrameRateCombo.setPreferredSize(comboSize);
        toFrameRateCombo.setMaximumSize(comboSize);

        // Button with limited width and height
        Dimension buttonSize = new Dimension(80, 40); // Narrower and lower
        detectFromVideoButton.setPreferredSize(buttonSize);
        detectFromVideoButton.setMaximumSize(buttonSize);
        detectFromVideoButton.setMinimumSize(buttonSize);

        tabbedPane = new JTabbedPane();
    }


    private void layoutComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // File selection section at the top
        add(createFileSelectionPanel(), BorderLayout.NORTH);

        // Tabs with different functionalities
        tabbedPane.addTab("⏰ Time Offset", createTimeOffsetPanel());
        tabbedPane.addTab("🎬 Frame Rate Conversion", createFrameRatePanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createFileSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.add(subtitleButton, BorderLayout.WEST);
        panel.add(subtitleLabel, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        return panel;
    }

    private JPanel createTimeOffsetPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Subtitle offset (seconds):"));
        panel.add(Box.createVerticalStrut(10));
        panel.add(createOffsetPanel());
        panel.add(Box.createVerticalStrut(5));
        panel.add(offsetValueLabel);
        panel.add(Box.createVerticalStrut(20));
        panel.add(saveButton);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createFrameRatePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // "From" section - without button
        panel.add(new JLabel("Subtitles are synchronized with:"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(fromFrameRateCombo);
        panel.add(Box.createVerticalStrut(15));

        // "To" section - with detection button
        panel.add(new JLabel("Convert to:"));
        panel.add(Box.createVerticalStrut(5));

        JPanel toPanel = new JPanel(new BorderLayout(5, 0));
        toPanel.add(toFrameRateCombo, BorderLayout.CENTER);
        toPanel.add(detectFromVideoButton, BorderLayout.EAST);
        panel.add(toPanel);

        panel.add(Box.createVerticalStrut(20));

        panel.add(saveFrameRateButton);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel createOffsetPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JButton minusButton = new JButton("−");
        JButton plusButton = new JButton("+");

        minusButton.addActionListener(e -> adjustOffset(-1));
        plusButton.addActionListener(e -> adjustOffset(+1));

        panel.add(minusButton, BorderLayout.WEST);
        panel.add(offsetSlider, BorderLayout.CENTER);
        panel.add(plusButton, BorderLayout.EAST);

        return panel;
    }

    private void setupEventHandlers() {
        subtitleButton.addActionListener(e -> handleFileSelection());
        saveButton.addActionListener(e -> handleSaveAction());
        saveFrameRateButton.addActionListener(e -> handleFrameRateConversion());
        detectFromVideoButton.addActionListener(e -> handleVideoFrameRateDetection());
        offsetSlider.addChangeListener(e -> updateOffsetLabel());
    }

    private void handleFileSelection() {
        File selectedFile = FileChooserHelper.chooseFile(this, "Select SRT File", "srt");

        if (selectedFile != null) {
            currentSubtitleFile = selectedFile;
            subtitleLabel.setText(selectedFile.getName());
        }
    }

    private void handleVideoFrameRateDetection() {
        String[] videoExtensions = {"mp4", "avi", "mkv", "mov", "wmv", "flv", "m4v"};
        File selectedFile = FileChooserHelper.chooseFile(this, "Select Video File", videoExtensions);

        if (selectedFile != null) {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                Optional<FrameRate> detectedFrameRate = videoMetadataService.detectFrameRate(selectedFile);

                if (detectedFrameRate.isPresent()) {
                    toFrameRateCombo.setSelectedItem(detectedFrameRate.get());
                    showSuccess("Frame rate detected: " + detectedFrameRate.get().getNameWithDescription());
                } else {
                    showError("Failed to detect frame rate from video file.\n" +
                            "Possible reasons:\n" +
                            "- Unsupported file format\n" +
                            "- Corrupted metadata\n" +
                            "- Missing frame rate information in file");
                }
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private void handleSaveAction() {
        if (currentSubtitleFile == null) {
            showError("No subtitle file selected.");
            return;
        }

        try {
            double offsetSeconds = offsetSlider.getValue();
            File outputFile = subtitleService.createShiftedSubtitles(currentSubtitleFile, offsetSeconds);
            showSuccess("Shifted subtitles saved as:\n" + outputFile.getName());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to process file while shifting subtitles: " + currentSubtitleFile, ex);
            showError("Failed to process file: " + ex.getMessage());
        }
    }

    private void handleFrameRateConversion() {
        if (currentSubtitleFile == null) {
            showError("No subtitle file selected.");
            return;
        }

        FrameRate fromFrameRate = (FrameRate) fromFrameRateCombo.getSelectedItem();
        FrameRate toFrameRate = (FrameRate) toFrameRateCombo.getSelectedItem();

        if (fromFrameRate == toFrameRate) {
            showError("Source and target frame rate are identical.");
            return;
        }

        try {
            File outputFile = subtitleService.createFrameRateConvertedSubtitles(
                    currentSubtitleFile, fromFrameRate, toFrameRate);
            showSuccess("Converted subtitles saved as:\n" + outputFile.getName());
        } catch (IllegalArgumentException ex) {
            log.log(Level.WARNING, "Parameter error during frame rate conversion for file: " + currentSubtitleFile, ex);
            showError("Parameter error: " + ex.getMessage());
        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO error during frame rate conversion for file: " + currentSubtitleFile, ex);
            showError("Failed to process file: " + ex.getMessage());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Unexpected error during frame rate conversion for file: " + currentSubtitleFile, ex);
            showError("An unexpected error occurred: " + ex.getMessage());
        }
    }

    private void adjustOffset(int delta) {
        int currentValue = offsetSlider.getValue();
        int newValue = Math.max(SLIDER_MIN, Math.min(SLIDER_MAX, currentValue + delta));
        offsetSlider.setValue(newValue);
    }

    private void updateOffsetLabel() {
        offsetValueLabel.setText(offsetSlider.getValue() + ".0 s");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }
}
