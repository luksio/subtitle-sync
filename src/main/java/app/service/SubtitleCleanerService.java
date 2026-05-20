package app.service;

import app.model.SubtitleEntry;
import io.vavr.control.Option;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class SubtitleCleanerService {

    private static final Pattern URL_PATTERN = Pattern.compile("(?:https?://|www\\.)\\S+");
    private static final Pattern DASH_PREFIX = Pattern.compile("^-\\s*");

    public List<SubtitleEntry> removeSpam(List<SubtitleEntry> entries) {
        return entries.stream()
                .filter(entry -> !containsUrl(entry.text()))
                .toList();
    }

    private boolean containsUrl(String text) {
        return URL_PATTERN.matcher(text).find();
    }

    public List<SubtitleEntry> removeSdh(List<SubtitleEntry> entries) {
        return entries.stream()
                .map(SubtitleCleanerService::cleanSdhFromEntry)
                .filter(Option::isDefined)
                .map(Option::get)
                .toList();
    }

    private Option<SubtitleEntry> cleanSdhFromEntry(SubtitleEntry entry) {
        String[] lines = entry.text().split("\n");
        List<String> cleanedLines = new ArrayList<>();
        // Per-line original dash prefix so untouched dialog lines round-trip byte-for-byte
        List<String> dashPrefixes = new ArrayList<>();

        for (String line : lines) {
            String dashPrefix = extractDashPrefix(line);
            String withoutDash = line.substring(dashPrefix.length());
            String processedLine = processLine(withoutDash);
            if (StringUtils.isNotBlank(processedLine)) {
                cleanedLines.add(processedLine);
                dashPrefixes.add(dashPrefix);
            }
        }

        // Restore each line's original dash prefix only when multiple dialog lines remain
        if (cleanedLines.size() > 1) {
            for (int i = 0; i < cleanedLines.size(); i++) {
                cleanedLines.set(i, dashPrefixes.get(i) + cleanedLines.get(i));
            }
        }

        String cleanedText = String.join("\n", cleanedLines);
        return Option.of(cleanedText)
                .filter(StringUtils::isNotBlank)
                .map(entry::withText);
    }

    private String extractDashPrefix(String line) {
        Matcher m = DASH_PREFIX.matcher(line);
        return m.find() ? m.group() : "";
    }

    private String processLine(String withoutDash) {
        if (SdhPatternMatcher.isOnlySdhContent(withoutDash)) {
            return "";
        }

        String cleaned = SdhPatternMatcher.cleanLine(withoutDash);

        // Cleaning may leave residual SDH (e.g. decorative ♪♪, an uppercase [SOUND] after speaker strip) — drop those too
        if (SdhPatternMatcher.isOnlySdhContent(cleaned)) {
            return "";
        }

        return cleaned;
    }
}
