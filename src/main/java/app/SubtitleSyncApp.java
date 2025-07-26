package app;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubtitleSyncApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dopasuj napisy do filmu");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(825, 260);
            frame.setLocationRelativeTo(null);

            SubtitleSyncPanel panel = new SubtitleSyncPanel();
            frame.setContentPane(panel);
            frame.setVisible(true);
        });
    }
}

class SubtitleSyncPanel extends JPanel {
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

class SubtitleService {

    public File createShiftedSubtitles(File inputFile, double offsetSeconds) throws IOException {
        List<SubtitleEntry> entries = parseSrt(inputFile);

        List<SubtitleEntry> shiftedEntries = entries.stream()
                .map(entry -> entry.shiftBySeconds(offsetSeconds))
                .toList();

        File outputFile = generateOutputFile(inputFile);

        writeSrt(outputFile, shiftedEntries);

        return outputFile;
    }

    private File generateOutputFile(File inputFile) {
        String name = inputFile.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = (dotIndex == -1) ? name : name.substring(0, dotIndex);
        String outputName = baseName + "_shifted.srt";

        return new File(inputFile.getParentFile(), outputName);
    }

    private List<SubtitleEntry> parseSrt(File file) throws IOException {
        List<SubtitleEntry> entries = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        int i = 0;

        while (i < lines.size()) {
            String line = lines.get(i).trim();

            if (i == 0 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
                line = line.substring(1);
            }

            if (line.matches("\\d+")) {
                int index = Integer.parseInt(line);
                String timeLine = lines.get(++i).trim();
                StringBuilder text = new StringBuilder();
                i++;
                while (i < lines.size() && !lines.get(i).isBlank()) {
                    text.append(lines.get(i++)).append("\n");
                }
                entries.add(SubtitleEntry.parse(index, timeLine, text.toString().trim()));
            } else {
                i++;
            }
        }

        return entries;
    }

    private void writeSrt(File file, List<SubtitleEntry> entries) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (SubtitleEntry entry : entries) {
                writer.write(entry.toSrtBlock());
                writer.write("\n\n");
            }
        }
    }
}

class FileChooserHelper {

    public static File chooseFile(Component parent, String title, String... extensions) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(title, extensions));

        int result = chooser.showOpenDialog(parent);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }
}

record SubtitleEntry(int index, Duration start, Duration end, String text) {

    static SubtitleEntry parse(int index, String timeLine, String text) {
        Pattern pattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})");
        Matcher m = pattern.matcher(timeLine);
        Duration start = Duration.ZERO;
        Duration end = Duration.ZERO;
        if (m.find()) start = toDuration(m);
        if (m.find()) end = toDuration(m);
        return new SubtitleEntry(index, start, end, text);
    }

    static Duration toDuration(Matcher m) {
        return Duration.ofHours(Integer.parseInt(m.group(1)))
                .plusMinutes(Integer.parseInt(m.group(2)))
                .plusSeconds(Integer.parseInt(m.group(3)))
                .plusMillis(Integer.parseInt(m.group(4)));
    }

    SubtitleEntry shiftBySeconds(double seconds) {
        Duration shift = Duration.ofMillis((long) (seconds * 1000));
        return new SubtitleEntry(index, start.plus(shift), end.plus(shift), text);
    }

    String toSrtBlock() {
        return "%d\n%s --> %s\n%s".formatted(index, formatTime(start), formatTime(end), text);
    }

    String formatTime(Duration duration) {
        return String.format("%02d:%02d:%02d,%03d",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart(),
                duration.toMillisPart());
    }
}