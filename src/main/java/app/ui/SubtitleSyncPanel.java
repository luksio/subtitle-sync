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

    // Komponenty UI
    private JLabel subtitleLabel;
    private JLabel offsetValueLabel;
    private JSlider offsetSlider;
    private JButton subtitleButton;
    private JButton saveButton;
    private JButton saveFrameRateButton;

    // Nowe komponenty dla frame rate
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
        subtitleLabel = new JLabel("Nie wybrano pliku z napisami");
        offsetValueLabel = new JLabel("0.0 s");

        subtitleButton = new JButton("Wybierz plik SRT");
        saveButton = new JButton("Zapisz przesunięte napisy");
        saveFrameRateButton = new JButton("Zapisz przekonwertowane napisy");

        // Przycisk z zawijaniem tekstu w HTML
        detectFromVideoButton = new JButton("<html><center>📹 Wykryj<br>z wideo</center></html>");

        offsetSlider = new JSlider(JSlider.HORIZONTAL, SLIDER_MIN, SLIDER_MAX, 0);
        offsetSlider.setMajorTickSpacing(SLIDER_MAJOR_TICK_SPACING);
        offsetSlider.setMinorTickSpacing(SLIDER_MINOR_TICK_SPACING);
        offsetSlider.setPaintTicks(true);
        offsetSlider.setPaintLabels(true);

        // Komponenty frame rate z ograniczoną szerokością
        fromFrameRateCombo = new JComboBox<>(FrameRate.values());
        toFrameRateCombo = new JComboBox<>(FrameRate.values());
        FrameRate defaultFrameRate = FrameRate.values()[0];
        fromFrameRateCombo.setSelectedItem(defaultFrameRate);
        toFrameRateCombo.setSelectedItem(defaultFrameRate);

        // Ograniczenie szerokości OBU combo boxów
        Dimension comboSize = new Dimension(300, fromFrameRateCombo.getPreferredSize().height);
        fromFrameRateCombo.setPreferredSize(comboSize);
        fromFrameRateCombo.setMaximumSize(comboSize);
        toFrameRateCombo.setPreferredSize(comboSize);
        toFrameRateCombo.setMaximumSize(comboSize);

        // Przycisk z ograniczoną szerokością i wysokością
        Dimension buttonSize = new Dimension(80, 40); // Węziej i niższy
        detectFromVideoButton.setPreferredSize(buttonSize);
        detectFromVideoButton.setMaximumSize(buttonSize);
        detectFromVideoButton.setMinimumSize(buttonSize);

        tabbedPane = new JTabbedPane();
    }


    private void layoutComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Sekcja wyboru pliku na górze
        add(createFileSelectionPanel(), BorderLayout.NORTH);

        // Zakładki z różnymi funkcjonalnościami
        tabbedPane.addTab("⏰ Przesunięcie czasowe", createTimeOffsetPanel());
        tabbedPane.addTab("🎬 Konwersja klatek", createFrameRatePanel());

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

        panel.add(new JLabel("Przesunięcie napisów (sekundy):"));
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

        // Sekcja "Z" - bez przycisku
        panel.add(new JLabel("Napisy są zsynchronizowane z:"));
        panel.add(Box.createVerticalStrut(5));
        panel.add(fromFrameRateCombo);
        panel.add(Box.createVerticalStrut(15));

        // Sekcja "Do" - z przyciskiem wykrywania
        panel.add(new JLabel("Chcę przekonwertować na:"));
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
        File selectedFile = FileChooserHelper.chooseFile(this, "Wybierz plik SRT", "srt");

        if (selectedFile != null) {
            currentSubtitleFile = selectedFile;
            subtitleLabel.setText(selectedFile.getName());
        }
    }

    private void handleVideoFrameRateDetection() {
        String[] videoExtensions = {"mp4", "avi", "mkv", "mov", "wmv", "flv", "m4v"};
        File selectedFile = FileChooserHelper.chooseFile(this, "Wybierz plik wideo", videoExtensions);

        if (selectedFile != null) {
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                Optional<FrameRate> detectedFrameRate = videoMetadataService.detectFrameRate(selectedFile);

                if (detectedFrameRate.isPresent()) {
                    toFrameRateCombo.setSelectedItem(detectedFrameRate.get());
                    showSuccess("Wykryto frame rate: " + detectedFrameRate.get().getNameWithDescription());
                } else {
                    showError("Nie udało się wykryć frame rate z pliku wideo.\n" +
                            "Możliwe przyczyny:\n" +
                            "- Nieobsługiwany format pliku\n" +
                            "- Uszkodzone metadane\n" +
                            "- Brak informacji o frame rate w pliku");
                }
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
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
            showSuccess("Zapisano przesunięte napisy jako:\n" + outputFile.getName());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Nie udało się przetworzyć pliku podczas przesuwania napisów: " + currentSubtitleFile, ex);
            showError("Nie udało się przetworzyć pliku: " + ex.getMessage());
        }
    }

    private void handleFrameRateConversion() {
        if (currentSubtitleFile == null) {
            showError("Nie wybrano pliku z napisami.");
            return;
        }

        FrameRate fromFrameRate = (FrameRate) fromFrameRateCombo.getSelectedItem();
        FrameRate toFrameRate = (FrameRate) toFrameRateCombo.getSelectedItem();

        if (fromFrameRate == toFrameRate) {
            showError("Źródłowy i docelowy frame rate są identyczne.");
            return;
        }

        try {
            File outputFile = subtitleService.createFrameRateConvertedSubtitles(
                    currentSubtitleFile, fromFrameRate, toFrameRate);
            showSuccess("Zapisano przekonwertowane napisy jako:\n" + outputFile.getName());
        } catch (IllegalArgumentException ex) {
            log.log(Level.WARNING, "Błąd parametrów przy konwersji frame rate dla pliku: " + currentSubtitleFile, ex);
            showError("Błąd parametrów: " + ex.getMessage());
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Błąd IO przy konwersji frame rate dla pliku: " + currentSubtitleFile, ex);
            showError("Nie udało się przetworzyć pliku: " + ex.getMessage());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Nieoczekiwany błąd przy konwersji frame rate dla pliku: " + currentSubtitleFile, ex);
            showError("Wystąpił nieoczekiwany błąd: " + ex.getMessage());
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
