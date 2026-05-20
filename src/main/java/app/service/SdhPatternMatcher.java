package app.service;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
class SdhPatternMatcher {

    // Optional outer ( ) tolerate real-world malformed SDH like "( CRYING )" wrapped in extra parens or "(MUSIC ♪))"
    private static final Pattern SOUND_DESCRIPTION_PARENS = Pattern.compile("^\\s*\\(?\\s*\\([^)]+\\)\\s*\\)?\\s*$");
    private static final Pattern SOUND_DESCRIPTION_BRACKETS = Pattern.compile("^\\s*\\[\\s*[^]]+\\s*]\\s*$");
    private static final Pattern ONLY_MUSIC_SYMBOLS = Pattern.compile("^[\\s♪]+$");
    private static final Pattern SPEAKER_NAME = Pattern.compile("^\\p{Lu}[\\p{Lu}\\d\\s#.'&-]+(?:\\([^)]+\\))?:\\s*");
    private static final Pattern SPEAKER_NAME_BRACKETS = Pattern.compile("^\\[([^]]+)]\\s*:?\\s*");
    private static final Pattern SOUND_IN_PARENS = Pattern.compile("\\s*\\([^)]+\\)\\s*");
    private static final Pattern MID_LINE_SOUND_BRACKET = Pattern.compile("\\s*\\[([a-z][^]]*)]\\s*");
    private static final Pattern ITALIC_TAGS = Pattern.compile("</?i>");
    private static final Pattern ITALIC_WRAPPER = Pattern.compile("^<i>(.*)</i>$");

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
        // Process italic-wrapped lines transparently so patterns can match the inner text.
        // Only trim/rewrap when something actually changed, otherwise preserve original whitespace.
        Matcher italicWrapper = ITALIC_WRAPPER.matcher(line);
        if (italicWrapper.matches()) {
            String originalInner = italicWrapper.group(1);
            String cleanedInner = applyPatterns(originalInner);
            if (cleanedInner.equals(originalInner)) {
                return line;
            }
            String trimmed = cleanedInner.trim();
            return trimmed.isBlank() ? "" : "<i>" + trimmed + "</i>";
        }
        return applyPatterns(line).trim();
    }

    private String applyPatterns(String line) {
        String result = line;

        // Remove speaker names (re-apply uppercase pattern after bracket speaker for chained labels like "[GIRL] MARY:")
        result = SPEAKER_NAME.matcher(result).replaceFirst("");
        result = stripBracketIfNotSongInfo(result, SPEAKER_NAME_BRACKETS);
        result = SPEAKER_NAME.matcher(result).replaceFirst("");

        // Remove sound descriptions in parens and brackets, gating each occurrence on song info
        result = SOUND_IN_PARENS.matcher(result)
                .replaceAll(m -> m.group().contains("\"") ? m.group() : " ");
        result = MID_LINE_SOUND_BRACKET.matcher(result)
                .replaceAll(m -> m.group(1).contains("\"") ? m.group() : " ");

        return result;
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
