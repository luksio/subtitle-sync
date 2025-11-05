package app.ui;

import app.model.FrameRate;
import app.presenter.SubtitleSyncPresenter;
import app.service.SubtitleService;
import app.service.VideoMetadataService;
import app.ui.view.SubtitleSyncView;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Optional;

public class SubtitleSyncPanel extends JPanel implements SubtitleSyncView {
    private static final int SLIDER_MIN = -60;
    private static final int SLIDER_MAX = 60;
    private static final int SLIDER_MAJOR_TICK_SPACING = 20;
    private static final int SLIDER_MINOR_TICK_SPACING = 5;

    private final SubtitleSyncPresenter presenter;
    private File currentSubtitleFile;

    private JLabel subtitleLabel;
    private JLabel offsetValueLabel;
    private JSlider offsetSlider;
    private JButton subtitleButton;
    private JButton saveButton;
    private JButton saveFrameRateButton;
    private JComboBox<FrameRate> fromFrameRateCombo;
    private JComboBox<FrameRate> toFrameRateCombo;
    private JButton detectFromVideoButton;
    private JTabbedPane tabbedPane;

    public SubtitleSyncPanel() {
        this.presenter = new SubtitleSyncPresenter(
                this,
                new SubtitleService(),
                new VideoMetadataService()
        );
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
        subtitleButton.addActionListener(e -> presenter.onSubtitleFileSelected());
        saveButton.addActionListener(e -> presenter.onSaveShiftedSubtitles());
        saveFrameRateButton.addActionListener(e -> presenter.onFrameRateConversion());
        detectFromVideoButton.addActionListener(e -> presenter.onDetectFrameRateFromVideo());
        offsetSlider.addChangeListener(e -> presenter.onOffsetChanged());
    }

    private void adjustOffset(int delta) {
        int currentValue = offsetSlider.getValue();
        int newValue = Math.max(SLIDER_MIN, Math.min(SLIDER_MAX, currentValue + delta));
        offsetSlider.setValue(newValue);
    }

    @Override
    public void setSubtitleFileName(String fileName) {
        subtitleLabel.setText(fileName);
    }

    @Override
    public void setOffsetValue(String offsetText) {
        offsetValueLabel.setText(offsetText);
    }

    @Override
    public void setFromFrameRate(FrameRate frameRate) {
        fromFrameRateCombo.setSelectedItem(frameRate);
    }

    @Override
    public void setToFrameRate(FrameRate frameRate) {
        toFrameRateCombo.setSelectedItem(frameRate);
    }

    @Override
    public File getCurrentSubtitleFile() {
        return currentSubtitleFile;
    }

    @Override
    public double getOffsetSeconds() {
        return offsetSlider.getValue();
    }

    @Override
    public FrameRate getFromFrameRate() {
        return (FrameRate) fromFrameRateCombo.getSelectedItem();
    }

    @Override
    public FrameRate getToFrameRate() {
        return (FrameRate) toFrameRateCombo.getSelectedItem();
    }

    @Override
    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void setBusy(boolean busy) {
        setCursor(busy ?
                Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) :
                Cursor.getDefaultCursor()
        );
    }

    @Override
    public Optional<File> chooseSubtitleFile() {
        File file = FileChooserHelper.chooseFile(this, "Select SRT File", "srt");
        if (file != null) {
            currentSubtitleFile = file;
        }
        return Optional.ofNullable(file);
    }

    @Override
    public Optional<File> chooseVideoFile() {
        String[] videoExtensions = {"mp4", "avi", "mkv", "mov", "wmv", "flv", "m4v"};
        return Optional.ofNullable(
                FileChooserHelper.chooseFile(this, "Select Video File", videoExtensions)
        );
    }
}
