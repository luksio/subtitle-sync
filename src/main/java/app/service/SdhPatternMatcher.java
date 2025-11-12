package app.service;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

@UtilityClass
class SdhPatternMatcher {

    private static final Pattern SOUND_DESCRIPTION_PARENS = Pattern.compile("^\\s*\\(?\\s*\\([^)]+\\)\\s*\\)?\\s*$");
    private static final Pattern SOUND_DESCRIPTION_BRACKETS = Pattern.compile("^\\s*\\[\\s*[^]]+\\s*]\\s*$");
    private static final Pattern ONLY_MUSIC_SYMBOLS = Pattern.compile("^[\\s♪]+$");
    private static final Pattern SPEAKER_NAME = Pattern.compile("^[A-Z][A-Z\\s]+(?:\\([^)]+\\))?:\\s*");
    private static final Pattern SPEAKER_NAME_BRACKETS = Pattern.compile("^\\[[^]]+]\\s*");
    private static final Pattern SOUND_IN_PARENS = Pattern.compile("\\s*\\([^)]+\\)\\s*");
    private static final Pattern SOUND_IN_BRACKETS = Pattern.compile("^\\s*\\[\\s*[a-z][^]]*]\\s*");
    private static final Pattern ITALIC_TAGS = Pattern.compile("</?i>");

    public boolean isOnlySdhContent(String line) {
        String cleaned = removeItalicTags(line).trim();

        if (StringUtils.isBlank(cleaned)) {
            return true;
        }

        if (SOUND_DESCRIPTION_PARENS.matcher(cleaned).matches()) {
            return true;
        }

        if (SOUND_DESCRIPTION_BRACKETS.matcher(cleaned).matches()) {
            return true;
        }

        if (ONLY_MUSIC_SYMBOLS.matcher(cleaned).matches()) {
            return true;
        }

        return false;
    }

    public String cleanLine(String line) {
        String result = line;

        // Remove speaker names
        result = SPEAKER_NAME.matcher(result).replaceFirst("");
        result = SPEAKER_NAME_BRACKETS.matcher(result).replaceFirst("");

        // Remove sound descriptions in brackets at the beginning
        result = SOUND_IN_BRACKETS.matcher(result).replaceFirst("");

        // Remove sound descriptions in parentheses (but not song info)
        if (!containsSongInfo(result)) {
            result = SOUND_IN_PARENS.matcher(result).replaceAll(" ");
        }

        return result.trim();
    }

    public boolean hasMusicSymbols(String line) {
        return line.contains("♪");
    }

    private boolean containsSongInfo(String line) {
        String cleaned = removeItalicTags(line);
        return cleaned.contains("\"") || cleaned.contains(" by ") || cleaned.contains("playing");
    }

    private String removeItalicTags(String text) {
        return ITALIC_TAGS.matcher(text).replaceAll("");
    }
}
