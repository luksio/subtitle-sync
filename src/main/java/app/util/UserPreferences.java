package app.util;

import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;

import java.io.File;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Persistent user-scoped settings backed by {@link Preferences} (OS-appropriate storage on each platform).
 */
@Log
@UtilityClass
public class UserPreferences {

    private static final Preferences PREFS = Preferences.userNodeForPackage(UserPreferences.class);
    private static final String LAST_DIRECTORY_KEY = "lastDirectory";

    /**
     * Returns the last directory the user picked a file from, if it still exists and is accessible.
     * Logs a warning when a saved path is no longer reachable (e.g. external drive disconnected).
     */
    public Optional<File> getLastDirectory() {
        String path = PREFS.get(LAST_DIRECTORY_KEY, null);
        if (path == null) {
            return Optional.empty();
        }
        File dir = new File(path);
        if (dir.isDirectory()) {
            return Optional.of(dir);
        }
        log.warning(() -> "Saved last directory is no longer accessible, falling back to default: " + path);
        return Optional.empty();
    }

    public void setLastDirectory(File directory) {
        if (directory != null && directory.isDirectory()) {
            PREFS.put(LAST_DIRECTORY_KEY, directory.getAbsolutePath());
        }
    }

    /**
     * Test-only: wipes stored preferences.
     */
    void clear() {
        PREFS.remove(LAST_DIRECTORY_KEY);
    }
}
