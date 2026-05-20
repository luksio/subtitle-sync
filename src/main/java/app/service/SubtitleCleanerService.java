package app.service;

import app.model.SubtitleEntry;
import io.vavr.control.Option;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@UtilityClass
public class SubtitleCleanerService {

    private static final Pattern URL_PATTERN = Pattern.compile("(?:https?://|www\\.)\\S+");

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
        List<Boolean> hadDash = new ArrayList<>();

        // First pass: clean lines and track which had dashes
        for (String line : lines) {
            boolean lineHadDash = line.trim().startsWith("-");
            String processedLine = processLine(line);
            if (StringUtils.isNotBlank(processedLine)) {
                cleanedLines.add(processedLine);
                hadDash.add(lineHadDash);
            }
        }

        // If multiple lines remain, restore dashes where they existed
        if (cleanedLines.size() > 1) {
            for (int i = 0; i < cleanedLines.size(); i++) {
                if (hadDash.get(i)) {
                    cleanedLines.set(i, "-" + cleanedLines.get(i));
                }
            }
        }

        String cleanedText = String.join("\n", cleanedLines);
        return Option.of(cleanedText)
                .filter(StringUtils::isNotBlank)
                .map(entry::withText);
    }

    private String processLine(String line) {
        String withoutDash = line.replaceFirst("^-\\s*", "");

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
