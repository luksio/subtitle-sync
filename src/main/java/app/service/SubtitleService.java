package app.service;

import app.model.FrameRate;
import app.model.SubtitleEntry;
import app.util.CharsetDetector;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Log
public class SubtitleService {

    private static final Charset DEFAULT_FALLBACK_CHARSET = Charset.forName("windows-1250");

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
        List<String> lines = readAllLines(file);
        int i = 0;

        while (i < lines.size()) {
            String line = StringUtils.trim(lines.get(i));

            // Handle BOM
            if (i == 0 && StringUtils.isNotEmpty(line) && line.charAt(0) == '\uFEFF') {
                line = line.substring(1);
            }

            if (StringUtils.isNumeric(line)) {
                int index = Integer.parseInt(line);
                String timeLine = StringUtils.trim(lines.get(++i));
                StringBuilder text = new StringBuilder();
                i++;
                while (i < lines.size() && StringUtils.isNotBlank(lines.get(i))) {
                    text.append(lines.get(i++)).append("\n");
                }
                entries.add(SubtitleEntry.parse(index, timeLine, StringUtils.trim(text.toString())));
            } else {
                i++;
            }
        }

        return entries;
    }

    private List<String> readAllLines(File file) throws IOException {
        Charset charset = CharsetDetector.detectCharsetWithFallback(
                file.toPath(),
                DEFAULT_FALLBACK_CHARSET
        );

        log.info(() -> "Wykryto kodowanie pliku '%s': %s".formatted(file.toPath(), charset.name()));
        return Files.readAllLines(file.toPath(), charset);
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
