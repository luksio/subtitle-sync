package app.service;

import app.model.SubtitleEntry;
import app.model.FrameRate;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SubtitleService {

    public File createShiftedSubtitles(File inputFile, double offsetSeconds) throws IOException {
        List<SubtitleEntry> entries = parseSrt(inputFile);
        List<SubtitleEntry> shiftedEntries = entries.stream()
                .map(entry -> entry.shiftBySeconds(offsetSeconds))
                .toList();

        File outputFile = generateOutputFile(inputFile, "_shifted");
        writeSrt(outputFile, shiftedEntries);
        return outputFile;
    }

    public File createFrameRateConvertedSubtitles(File inputFile, FrameRate fromFrameRate, FrameRate toFrameRate) throws IOException {
        if (fromFrameRate == toFrameRate) {
            throw new IllegalArgumentException("Źródłowy i docelowy frame rate są identyczne");
        }

        List<SubtitleEntry> entries = parseSrt(inputFile);
        BigDecimal conversionRatio = FrameRate.getPreciseConversionRatio(fromFrameRate, toFrameRate);

        List<SubtitleEntry> convertedEntries = entries.stream()
                .map(entry -> entry.convertFrameRate(conversionRatio))
                .toList();

        String suffix = String.format("_%s_to_%s",
                fromFrameRate.getNameWithFpsSuffix().replace(" ", "_").replace(".", "_"),
                toFrameRate.getNameWithFpsSuffix().replace(" ", "_").replace(".", "_"));

        File outputFile = generateOutputFile(inputFile, suffix);
        writeSrt(outputFile, convertedEntries);
        return outputFile;
    }

    private File generateOutputFile(File inputFile, String suffix) {
        String name = inputFile.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = (dotIndex == -1) ? name : name.substring(0, dotIndex);
        String outputName = baseName + suffix + ".srt";

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
