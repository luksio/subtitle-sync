package app.service;

import java.io.File;
import java.util.Optional;

/**
 * Outcome of a subtitle cleaning operation. Removal counts cover entries dropped entirely;
 * the modified count covers entries whose text was changed in place (e.g. speaker prefix stripped).
 * outputFile is empty when the input contained no SDH/spam to clean — no point writing an identical copy.
 */
public record CleanResult(Optional<File> outputFile, File changesFile, int sdhRemoved, int spamRemoved, int modified) {
}
