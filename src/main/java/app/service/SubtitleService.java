package app.service;

import app.model.FrameRate;
import app.model.SubtitleEntry;
import app.service.SubtitleChanges.ModifiedEntry;
import app.service.SubtitleChanges.RemovalReason;
import app.service.SubtitleChanges.RemovedEntry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public CleanResult createCleanedSubtitles(File inputFile, boolean removeSdh, boolean removeSpam) throws IOException {
        if (!removeSdh && !removeSpam) {
            throw new IllegalArgumentException("At least one cleaning option must be selected");
        }

        List<SubtitleEntry> original = SubtitleParserService.parseFile(inputFile);
        List<SubtitleEntry> afterSdh = removeSdh ? SubtitleCleanerService.removeSdh(original) : original;
        List<SubtitleEntry> afterSpam = removeSpam ? SubtitleCleanerService.removeSpam(afterSdh) : afterSdh;

        File potentialOutput = generateOutputFile(inputFile, suffixFor(removeSdh, removeSpam));
        SubtitleChanges changes = computeChanges(inputFile, removeSdh, removeSpam, original, afterSdh, afterSpam);

        // Skip writing the output SRT when nothing actually changed — an identical copy is just noise
        Optional<File> writtenOutput = Optional.empty();
        if (!changes.removedEntries().isEmpty() || !changes.modifiedEntries().isEmpty()) {
            writeSrt(potentialOutput, afterSpam);
            writtenOutput = Optional.of(potentialOutput);
        }

        SubtitleChanges changesWithOutput = withOutput(changes, writtenOutput);
        File changesFile = changesLogFor(potentialOutput);
        SubtitleChangesLogWriter.write(changesFile, changesWithOutput);

        long sdhRemoved = changes.removedEntries().stream().filter(re -> re.reason() == RemovalReason.SDH).count();
        long spamRemoved = changes.removedEntries().stream().filter(re -> re.reason() == RemovalReason.SPAM).count();
        return new CleanResult(writtenOutput, changesFile, (int) sdhRemoved, (int) spamRemoved, changes.modifiedEntries().size());
    }

    private SubtitleChanges withOutput(SubtitleChanges c, Optional<File> outputFile) {
        return new SubtitleChanges(c.inputFile(), outputFile, c.removedSdh(), c.removedSpam(),
                c.removedEntries(), c.modifiedEntries(), c.unchangedCount());
    }

    private SubtitleChanges computeChanges(File inputFile, boolean removeSdh, boolean removeSpam,
                                           List<SubtitleEntry> original, List<SubtitleEntry> afterSdh,
                                           List<SubtitleEntry> afterSpam) {
        Set<Integer> survivedSdh = indices(afterSdh);
        Set<Integer> survivedSpam = indices(afterSpam);

        List<RemovedEntry> removed = new ArrayList<>();
        for (SubtitleEntry origEntry : original) {
            if (!survivedSdh.contains(origEntry.index())) {
                removed.add(new RemovedEntry(origEntry, RemovalReason.SDH));
            } else if (!survivedSpam.contains(origEntry.index())) {
                removed.add(new RemovedEntry(origEntry, RemovalReason.SPAM));
            }
        }

        Map<Integer, SubtitleEntry> originalByIndex = original.stream()
                .collect(Collectors.toMap(SubtitleEntry::index, Function.identity()));
        List<ModifiedEntry> modified = new ArrayList<>();
        for (SubtitleEntry finalEntry : afterSpam) {
            SubtitleEntry origEntry = originalByIndex.get(finalEntry.index());
            if (origEntry != null && !origEntry.text().equals(finalEntry.text())) {
                modified.add(new ModifiedEntry(origEntry, finalEntry));
            }
        }

        int unchanged = afterSpam.size() - modified.size();
        return new SubtitleChanges(inputFile, Optional.empty(), removeSdh, removeSpam, removed, modified, unchanged);
    }

    private Set<Integer> indices(List<SubtitleEntry> entries) {
        Set<Integer> result = new HashSet<>();
        for (SubtitleEntry e : entries) result.add(e.index());
        return result;
    }

    private String suffixFor(boolean removeSdh, boolean removeSpam) {
        if (removeSdh && removeSpam) return "_cleaned";
        if (removeSdh) return "_no_sdh";
        return "_no_spam";
    }

    private File changesLogFor(File outputFile) {
        String name = outputFile.getName();
        int dotIndex = name.lastIndexOf('.');
        String baseName = (dotIndex == -1) ? name : name.substring(0, dotIndex);
        return new File(outputFile.getParentFile(), baseName + "_changes.log");
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
