package app;

import app.ui.SubtitleSyncPanel;

import javax.swing.*;

public class SubtitleSyncApp {
    public static void main(String[] args) {
        // Follow system appearance on macOS (dark mode picker + window). No-op on other OSes.
        System.setProperty("apple.awt.application.appearance", "system");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Subtitle Sync");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 400);
            frame.setLocationRelativeTo(null);

            SubtitleSyncPanel panel = new SubtitleSyncPanel();
            frame.setContentPane(panel);
            frame.setVisible(true);
        });
    }
}
