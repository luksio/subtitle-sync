package app.service;

import app.model.SubtitleEntry;
import app.util.CharsetDetector;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for parsing SRT subtitle files into SubtitleEntry objects.
 */
@Log
@UtilityClass
public class SubtitleParserService {

    private static final Charset DEFAULT_FALLBACK_CHARSET = Charset.forName("windows-1250");

    /**
     * Parses an SRT file into a list of SubtitleEntry objects.
     * Automatically detects the file encoding using CharsetDetector.
     *
     * @param file the SRT file to parse
     * @return a list of parsed subtitle entries
     * @throws IOException if an I/O error occurs reading the file
     */
    public List<SubtitleEntry> parseFile(File file) throws IOException {
        List<String> lines = readAllLines(file);
        return parseSrtLines(lines);
    }

    private List<String> readAllLines(File file) throws IOException {
        Charset charset = CharsetDetector.detectCharsetWithFallback(
                file.toPath(),
                DEFAULT_FALLBACK_CHARSET
        );

        log.info(() -> "Detected file encoding '%s': %s".formatted(file.toPath(), charset.name()));
        return Files.readAllLines(file.toPath(), charset);
    }

    private List<SubtitleEntry> parseSrtLines(List<String> lines) {
        List<SubtitleEntry> entries = new ArrayList<>();
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
}
