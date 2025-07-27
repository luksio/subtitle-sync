
package app.ui;

import app.service.SubtitleService;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class SubtitleSyncPanel extends JPanel {
    private static final int SLIDER_MIN = -60;
    private static final int SLIDER_MAX = 60;
    private static final int SLIDER_MAJOR_TICK_SPACING = 20;
    private static final int SLIDER_MINOR_TICK_SPACING = 5;

    private final SubtitleService subtitleService;
    private File currentSubtitleFile;

    private JLabel subtitleLabel;
    private JLabel offsetValueLabel;
    private JSlider offsetSlider;
    private JButton subtitleButton;
    private JButton saveButton;

    public SubtitleSyncPanel() {
        this.subtitleService = new SubtitleService();
        initializeComponents();
        layoutComponents();
        setupEventHandlers();
    }

    private void initializeComponents() {
        subtitleLabel = new JLabel("Nie wybrano pliku z napisami");
        offsetValueLabel = new JLabel("0.0 s");

        subtitleButton = new JButton("Wybierz plik SRT");
        saveButton = new JButton("Zapisz nowe napisy");

        offsetSlider = new JSlider(JSlider.HORIZONTAL, SLIDER_MIN, SLIDER_MAX, 0);
        offsetSlider.setMajorTickSpacing(SLIDER_MAJOR_TICK_SPACING);
        offsetSlider.setMinorTickSpacing(SLIDER_MINOR_TICK_SPACING);
        offsetSlider.setPaintTicks(true);
        offsetSlider.setPaintLabels(true);
    }

    private void layoutComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Sekcja wyboru pliku
        add(createFileSelectionPanel());
        add(Box.createVerticalStrut(10));

        // Sekcja przesunięcia
        add(new JLabel("Przesunięcie napisów (sekundy):"));
        add(createOffsetPanel());
        add(offsetValueLabel);

        add(Box.createVerticalStrut(15));
        add(saveButton);
    }

    private JPanel createFileSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.add(subtitleButton, BorderLayout.WEST);
        panel.add(subtitleLabel, BorderLayout.CENTER);
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
        offsetSlider.addChangeListener(e -> updateOffsetLabel());
    }

    private void handleFileSelection() {
        File selectedFile = FileChooserHelper.chooseFile(this, "Wybierz plik SRT", "srt");

        if (selectedFile != null) {
            currentSubtitleFile = selectedFile;
            subtitleLabel.setText(selectedFile.getName());
        }
    }

    private void handleSaveAction() {
        if (currentSubtitleFile == null) {
            showError("Nie wybrano pliku z napisami.");
            return;
        }

        try {
            double offsetSeconds = offsetSlider.getValue();
            File outputFile = subtitleService.createShiftedSubtitles(currentSubtitleFile, offsetSeconds);

            showSuccess("Zapisano jako:\n" + outputFile.getName());
        } catch (IOException ex) {
            showError("Nie udało się przetworzyć pliku: " + ex.getMessage());
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
        JOptionPane.showMessageDialog(this, message, "Błąd", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Sukces", JOptionPane.INFORMATION_MESSAGE);
    }
}
