package app.service;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
class SdhPatternMatcher {

    private static final Pattern SOUND_DESCRIPTION_PARENS = Pattern.compile("^\\s*\\(?\\s*\\([^)]+\\)\\s*\\)?\\s*$");
    private static final Pattern SOUND_DESCRIPTION_BRACKETS = Pattern.compile("^\\s*\\[\\s*[^]]+\\s*]\\s*$");
    private static final Pattern ONLY_MUSIC_SYMBOLS = Pattern.compile("^[\\s♪]+$");
    private static final Pattern SPEAKER_NAME = Pattern.compile("^\\p{Lu}[\\p{Lu}\\d\\s#.'&-]+(?:\\([^)]+\\))?:\\s*");
    private static final Pattern SPEAKER_NAME_BRACKETS = Pattern.compile("^\\[([^]]+)]\\s*:?\\s*");
    private static final Pattern SOUND_IN_PARENS = Pattern.compile("\\s*\\([^)]+\\)\\s*");
    private static final Pattern SOUND_IN_BRACKETS = Pattern.compile("^\\s*\\[\\s*([a-z][^]]*)]\\s*");
    private static final Pattern ITALIC_TAGS = Pattern.compile("</?i>");

    public boolean isOnlySdhContent(String line) {
        String cleaned = removeItalicTags(line).trim();

        if (StringUtils.isBlank(cleaned)) {
            return true;
        }

        // Quoted text indicates song info (title/lyrics), not SDH — never strip those entries
        if (containsSongInfo(cleaned)) {
            return false;
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

        // Remove speaker names (re-apply uppercase pattern after bracket speaker for chained labels like "[GIRL] MARY:")
        result = SPEAKER_NAME.matcher(result).replaceFirst("");
        result = stripBracketIfNotSongInfo(result, SPEAKER_NAME_BRACKETS);
        result = SPEAKER_NAME.matcher(result).replaceFirst("");

        // Remove sound descriptions in brackets at the beginning
        result = stripBracketIfNotSongInfo(result, SOUND_IN_BRACKETS);

        // Remove sound descriptions in parentheses (but not song info)
        if (!containsSongInfo(result)) {
            result = SOUND_IN_PARENS.matcher(result).replaceAll(" ");
        }

        return result.trim();
    }

    private String stripBracketIfNotSongInfo(String line, Pattern pattern) {
        Matcher m = pattern.matcher(line);
        if (m.find() && !m.group(1).contains("\"")) {
            return m.replaceFirst("");
        }
        return line;
    }

    private boolean containsSongInfo(String line) {
        return removeItalicTags(line).contains("\"");
    }

    private String removeItalicTags(String text) {
        return ITALIC_TAGS.matcher(text).replaceAll("");
    }
}
