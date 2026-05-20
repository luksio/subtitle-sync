package app.service;

import app.service.SubtitleChanges.ModifiedEntry;
import app.service.SubtitleChanges.RemovalReason;
import app.service.SubtitleChanges.RemovedEntry;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
class SubtitleChangesLogWriter {

    public void write(File destination, SubtitleChanges changes) throws IOException {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, changes);
        appendSummary(sb, changes);
        appendRemoved(sb, changes);
        appendModified(sb, changes);
        Files.writeString(destination.toPath(), sb.toString(), StandardCharsets.UTF_8);
    }

    private void appendHeader(StringBuilder sb, SubtitleChanges c) {
        sb.append("Subtitle Cleaning Report\n");
        sb.append("========================\n");
        sb.append("Input:   ").append(c.inputFile().getName()).append("\n");
        sb.append("Output:  ").append(c.outputFile().getName()).append("\n");
        sb.append("Options: ").append(formatOptions(c)).append("\n\n");
    }

    private String formatOptions(SubtitleChanges c) {
        List<String> opts = new ArrayList<>();
        if (c.removedSdh()) opts.add("Remove SDH");
        if (c.removedSpam()) opts.add("Remove spam");
        return String.join(", ", opts);
    }

    private void appendSummary(StringBuilder sb, SubtitleChanges c) {
        long sdh = c.removedEntries().stream().filter(re -> re.reason() == RemovalReason.SDH).count();
        long spam = c.removedEntries().stream().filter(re -> re.reason() == RemovalReason.SPAM).count();
        int modified = c.modifiedEntries().size();

        sb.append("Summary\n");
        sb.append("-------\n");
        sb.append(String.format("Removed via SDH:    %d %s%n", sdh, plural(sdh)));
        sb.append(String.format("Removed via spam:   %d %s%n", spam, plural(spam)));
        sb.append(String.format("Modified by SDH:    %d %s%n", modified, plural(modified)));
        sb.append(String.format("Kept unchanged:     %d %s%n", c.unchangedCount(), plural(c.unchangedCount())));

        if (sdh == 0 && spam == 0 && modified == 0) {
            sb.append("\nNo changes were made.\n");
        }
        sb.append("\n");
    }

    private String plural(long count) {
        return count == 1 ? "entry" : "entries";
    }

    private void appendRemoved(StringBuilder sb, SubtitleChanges c) {
        if (c.removedEntries().isEmpty()) {
            return;
        }
        sb.append("Removed entries\n");
        sb.append("---------------\n\n");
        for (RemovedEntry re : c.removedEntries()) {
            sb.append(String.format("#%d  %s  (via %s)%n",
                    re.entry().index(),
                    re.entry().formattedTimeline(),
                    re.reason().displayName()));
            appendIndented(sb, re.entry().text(), "    ");
            sb.append("\n");
        }
    }

    private void appendModified(StringBuilder sb, SubtitleChanges c) {
        if (c.modifiedEntries().isEmpty()) {
            return;
        }
        sb.append("Modified entries\n");
        sb.append("----------------\n\n");
        for (ModifiedEntry me : c.modifiedEntries()) {
            sb.append(String.format("#%d  %s%n", me.before().index(), me.before().formattedTimeline()));
            appendIndented(sb, me.before().text(), "    ");
            sb.append("  →\n");
            appendIndented(sb, me.after().text(), "    ");
            sb.append("\n");
        }
    }

    private void appendIndented(StringBuilder sb, String text, String indent) {
        for (String line : text.split("\n", -1)) {
            sb.append(indent).append(line).append("\n");
        }
    }
}
