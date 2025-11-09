package app.service;

import app.model.SubtitleEntry;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * Service for cleaning subtitle entries by removing spam content and
 * SDH (Subtitles for Deaf and Hard of hearing) annotations.
 */
@UtilityClass
public class SubtitleCleanerService {

    /**
     * Removes spam content from subtitle entries (e.g., URLs, rip information).
     *
     * @param entries the list of subtitle entries to clean
     * @return a new list with spam content removed
     */
    public List<SubtitleEntry> removeSpam(List<SubtitleEntry> entries) {
        // TODO: implement spam removal (URLs, etc.)
        return entries;
    }

    /**
     * Removes SDH (Subtitles for Deaf and Hard of hearing) annotations from subtitle entries.
     * This includes sound descriptions in parentheses, brackets, speaker names, etc.
     *
     * @param entries the list of subtitle entries to clean
     * @return a new list with SDH content removed; entries that contain only SDH content are removed entirely
     */
    public List<SubtitleEntry> removeSDH(List<SubtitleEntry> entries) {
        // TODO: implement SDH removal
        return entries;
    }
}
