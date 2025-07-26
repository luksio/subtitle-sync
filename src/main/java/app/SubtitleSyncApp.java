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

    private static File subtitleFile;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SubtitleSyncApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Dopasuj napisy do filmu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(825, 260);
        frame.setLocationRelativeTo(null);

        JLabel subtitleLabel = new JLabel("Nie wybrano pliku z napisami");
        JLabel offsetValueLabel = new JLabel("0.0 s");

        JButton subtitleButton = new JButton("Wybierz plik SRT");
        subtitleButton.addActionListener(e -> {
            File selected = chooseFile(frame, "Wybierz plik SRT", "srt");
            if (selected != null) {
                subtitleFile = selected;
                subtitleLabel.setText(selected.getName());
            }
        });

        JSlider slider = new JSlider(JSlider.HORIZONTAL, -60, 60, 0);
        slider.setMajorTickSpacing(20);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.addChangeListener(e -> offsetValueLabel.setText(slider.getValue() + ".0 s"));

        JButton saveButton = new JButton("Zapisz nowe napisy");
        saveButton.addActionListener(event -> {
            if (subtitleFile == null) {
                showMessage(frame, "Nie wybrano pliku z napisami.", "Błąd");
                return;
            }

            List<SubtitleEntry> entries;
            try {
                entries = parseSrt(subtitleFile);
            } catch (IOException ex) {
                showMessage(frame, "Nie udało się wczytać pliku z napisami.", "Błąd");
                return;
            }

            double offsetSeconds = slider.getValue();
            List<SubtitleEntry> shifted = entries.stream()
                    .map(entry -> entry.shiftBySeconds(offsetSeconds))
                    .toList();

            String name = subtitleFile.getName();
            int dot = name.lastIndexOf('.');
            String base = (dot == -1) ? name : name.substring(0, dot);
            String shiftedName = base + "_shifted.srt";
            File outputFile = new File(subtitleFile.getParentFile(), shiftedName);

            try {
                writeSrt(outputFile, shifted);
                showMessage(frame, "Zapisano jako:\n" + shiftedName, "Sukces");
            } catch (IOException ex) {
                showMessage(frame, "Nie udało się zapisać pliku.", "Błąd");
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(buildRow(subtitleButton, subtitleLabel));
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Przesunięcie napisów (sekundy):"));

        JPanel sliderPanel = new JPanel(new BorderLayout());
        JButton minusButton = new JButton("−");
        JButton plusButton = new JButton("+");

        minusButton.addActionListener(e -> {
            int val = slider.getValue();
            if (val > slider.getMinimum()) {
                slider.setValue(val - 1);
            }
        });

        plusButton.addActionListener(e -> {
            int val = slider.getValue();
            if (val < slider.getMaximum()) {
                slider.setValue(val + 1);
            }
        });

        sliderPanel.add(minusButton, BorderLayout.WEST);
        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderPanel.add(plusButton, BorderLayout.EAST);

        panel.add(sliderPanel);
        panel.add(offsetValueLabel);

        panel.add(Box.createVerticalStrut(15));
        panel.add(saveButton);

        frame.setContentPane(panel);
        frame.setVisible(true);
    }

    private static JPanel buildRow(JComponent left, JComponent right) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.CENTER);
        return row;
    }

    private static File chooseFile(Component parent, String title, String... extensions) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(title, extensions));
        int result = chooser.showOpenDialog(parent);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    private static void showMessage(Component parent, String msg, String title) {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private static List<SubtitleEntry> parseSrt(File file) throws IOException {
        List<SubtitleEntry> list = new ArrayList<>();
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
                list.add(SubtitleEntry.parse(index, timeLine, text.toString().trim()));
            } else {
                i++;
            }
        }

        return list;
    }

    private static void writeSrt(File file, List<SubtitleEntry> entries) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (SubtitleEntry e : entries) {
                writer.write(e.toSrtBlock());
                writer.write("\n\n");
            }
        }
    }

    private record SubtitleEntry(int index, Duration start, Duration end, String text) {

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

        String formatTime(Duration d) {
            long h = d.toHours();
            long m = d.toMinutesPart();
            long s = d.toSecondsPart();
            long ms = d.toMillisPart();
            return String.format("%02d:%02d:%02d,%03d", h, m, s, ms);
        }
    }
}
