package app.service;

import java.io.File;

/**
 * Outcome of a subtitle cleaning operation. Counts cover entries fully dropped from the file;
 * entries that were modified in place (e.g. speaker prefix stripped but dialog kept) are not counted.
 */
public record CleanResult(File outputFile, int sdhRemoved, int spamRemoved) {
}
