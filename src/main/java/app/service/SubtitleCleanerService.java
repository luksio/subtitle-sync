package app.service;

import app.model.SubtitleEntry;
import lombok.experimental.UtilityClass;

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
        // TODO: implement SDH removal
        return entries;
    }
}
