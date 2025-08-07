package app.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class FileChooserHelper {

    public static File chooseFile(Component parent, String title, String... extensions) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);

        if (extensions.length == 1) {
            // Pojedyncze rozszerzenie
            String ext = extensions[0];
            String description = ext.toUpperCase() + " files (*." + ext + ")";
            FileNameExtensionFilter filter = new FileNameExtensionFilter(description, ext);
            fileChooser.setFileFilter(filter);
        } else if (extensions.length > 1) {
            // Wiele rozszerzeń
            StringBuilder description = new StringBuilder("Obsługiwane pliki (");
            for (int i = 0; i < extensions.length; i++) {
                if (i > 0) description.append(", ");
                description.append("*.").append(extensions[i]);
            }
            description.append(")");

            FileNameExtensionFilter filter = new FileNameExtensionFilter(description.toString(), extensions);
            fileChooser.setFileFilter(filter);
        }

        int result = fileChooser.showOpenDialog(parent);
        return (result == JFileChooser.APPROVE_OPTION) ? fileChooser.getSelectedFile() : null;
    }
}
