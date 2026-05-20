package app;

import app.ui.SubtitleSyncPanel;

import javax.swing.*;
import java.awt.*;

public class SubtitleSyncApp {
    public static void main(String[] args) {
        // Follow system appearance on macOS (dark mode picker + window). No-op on other OSes.
        System.setProperty("apple.awt.application.appearance", "system");

        SwingUtilities.invokeLater(() -> {
            // Aqua L&F partially honours dark mode and renders the selected tab title in near-white on a light background
            UIManager.put("TabbedPane.selectedTabTitleNormalColor", Color.BLACK);

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
