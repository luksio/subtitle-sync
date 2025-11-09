package app.service;

import app.model.FrameRate;
import app.model.SubtitleEntry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class SubtitleService {

    public File createShiftedSubtitles(File inputFile, double offsetSeconds) throws IOException {
        List<SubtitleEntry> entries = SubtitleParserService.parseFile(inputFile);
        List<SubtitleEntry> shiftedEntries = entries.stream()
                .map(entry -> entry.shiftBySeconds(offsetSeconds))
                .toList();

        File outputFile = generateOutputFile(inputFile, "_shifted");
        writeSrt(outputFile, shiftedEntries);
        return outputFile;
    }

    public File createFrameRateConvertedSubtitles(File inputFile, FrameRate fromFrameRate, FrameRate toFrameRate) throws IOException {
        if (fromFrameRate == toFrameRate) {
            throw new IllegalArgumentException("Source and target frame rate are identical");
        }

        List<SubtitleEntry> entries = SubtitleParserService.parseFile(inputFile);
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

    private void writeSrt(File file, List<SubtitleEntry> entries) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            for (SubtitleEntry entry : entries) {
                writer.write(entry.toSrtBlock());
                writer.write("\n\n");
            }
        }
    }
}
