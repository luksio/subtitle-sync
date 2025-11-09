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

@UtilityClass
public class TestFileUtils {

    public File createTempSrtFile(Path tempDir, String fileName, String content) {
        File file = tempDir.resolve(fileName).toFile();
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp SRT file: " + fileName, e);
        }
        return file;
    }

    public File createTempSrtFileWithEncoding(Path tempDir, String fileName, String content, Charset charset) {
        File file = tempDir.resolve(fileName).toFile();
        try {
            Files.write(file.toPath(), content.getBytes(charset));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp SRT file with encoding: " + fileName, e);
        }
        return file;
    }

    public List<SubtitleEntry> parseTestSrt(Path tempDir, String srtContent) {
        File file = createTempSrtFile(tempDir, "test_parse.srt", srtContent);
        try {
            return SubtitleParserService.parseFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse test SRT content", e);
        }
    }
}
