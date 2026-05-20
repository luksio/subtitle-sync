package app.service;

import app.model.SubtitleEntry;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Aggregated diff of a subtitle cleaning operation. Drives the changes log written next to the output file.
 * outputFile is empty when no output SRT was written (input contained nothing to clean).
 */
public record SubtitleChanges(
        File inputFile,
        Optional<File> outputFile,
        boolean removedSdh,
        boolean removedSpam,
        List<RemovedEntry> removedEntries,
        List<ModifiedEntry> modifiedEntries,
        int unchangedCount
) {

    public enum RemovalReason {
        SDH("SDH"),
        SPAM("spam");

        private final String displayName;

        RemovalReason(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public record RemovedEntry(SubtitleEntry entry, RemovalReason reason) {
    }

    public record ModifiedEntry(SubtitleEntry before, SubtitleEntry after) {
    }
}
