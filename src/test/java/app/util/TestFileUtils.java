package app.util;

import app.model.SubtitleEntry;
import app.service.SubtitleParserService;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Test utility class for creating temporary SRT files and parsing test content.
 * Provides helper methods to simplify test setup for subtitle-related tests.
 */
@UtilityClass
public class TestFileUtils {

    /**
     * Creates a temporary SRT file with the given content.
     *
     * @param tempDir  the temporary directory to create the file in
     * @param fileName the name of the file to create
     * @param content  the content to write to the file
     * @return the created file
     */
    public File createTempSrtFile(Path tempDir, String fileName, String content) {
        File file = tempDir.resolve(fileName).toFile();
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp SRT file: " + fileName, e);
        }
        return file;
    }

    /**
     * Creates a temporary SRT file with the given content and specific encoding.
     *
     * @param tempDir  the temporary directory to create the file in
     * @param fileName the name of the file to create
     * @param content  the content to write to the file
     * @param charset  the charset to use for encoding
     * @return the created file
     */
    public File createTempSrtFileWithEncoding(Path tempDir, String fileName, String content, Charset charset) {
        File file = tempDir.resolve(fileName).toFile();
        try {
            Files.write(file.toPath(), content.getBytes(charset));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp SRT file with encoding: " + fileName, e);
        }
        return file;
    }

    /**
     * Parses SRT content from a string into a list of SubtitleEntry objects.
     * This is a convenience method for tests that creates a temporary file,
     * parses it, and returns the entries.
     *
     * @param tempDir    the temporary directory to use for the intermediate file
     * @param srtContent the SRT content to parse
     * @return a list of parsed subtitle entries
     */
    public List<SubtitleEntry> parseTestSrt(Path tempDir, String srtContent) {
        File file = createTempSrtFile(tempDir, "test_parse.srt", srtContent);
        try {
            return SubtitleParserService.parseFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse test SRT content", e);
        }
    }
}
