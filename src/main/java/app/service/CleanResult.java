package app.service;

import java.io.File;

/**
 * Outcome of a subtitle cleaning operation. Removal counts cover entries dropped entirely;
 * the modified count covers entries whose text was changed in place (e.g. speaker prefix stripped).
 */
public record CleanResult(File outputFile, File changesFile, int sdhRemoved, int spamRemoved, int modified) {
}
