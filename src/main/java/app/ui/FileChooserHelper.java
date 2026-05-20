package app.ui;

import app.util.UserPreferences;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

public class FileChooserHelper {

    public static File chooseFile(Component parent, String title, String... extensions) {
        // FileDialog uses the OS-native picker (Finder on macOS, Win32 on Windows) instead of Swing's JFileChooser
        FileDialog dialog = new FileDialog(JOptionPane.getFrameForComponent(parent), title, FileDialog.LOAD);
        UserPreferences.getLastDirectory()
                .ifPresent(dir -> dialog.setDirectory(dir.getAbsolutePath()));
        if (extensions.length > 0) {
            dialog.setFilenameFilter(extensionFilter(extensions));
        }

        dialog.setVisible(true);

        String filename = dialog.getFile();
        String directory = dialog.getDirectory();
        if (filename == null || directory == null) {
            return null;
        }
        File selected = new File(directory, filename);
        UserPreferences.setLastDirectory(selected.getParentFile());
        return selected;
    }

    private static FilenameFilter extensionFilter(String[] extensions) {
        String[] lowered = Arrays.stream(extensions).map(String::toLowerCase).toArray(String[]::new);
        return (dir, name) -> {
            String lowerName = name.toLowerCase();
            for (String ext : lowered) {
                if (lowerName.endsWith("." + ext)) return true;
            }
            return false;
        };
    }
}
