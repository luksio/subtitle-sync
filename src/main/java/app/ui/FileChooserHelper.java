package app.ui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class FileChooserHelper {

    public static File chooseFile(Component parent, String title, String... extensions) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter(title, extensions));

        int result = chooser.showOpenDialog(parent);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }
}
