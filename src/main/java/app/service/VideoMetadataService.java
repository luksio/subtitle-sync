package app.service;

import app.model.FrameRate;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class VideoMetadataService {

    public Optional<FrameRate> detectFrameRate(File videoFile) {
        if (videoFile == null || !videoFile.exists()) {
            return Optional.empty();
        }

        try {
            if (!isFFprobeAvailable()) {
                log.warning("FFprobe nie jest dostępne w systemie");
                return Optional.empty();
            }

            double fps = extractFrameRateWithFFprobe(videoFile);
            return findClosestFrameRate(fps);
        } catch (Exception e) {
            log.log(Level.WARNING, "Nie udało się odczytać frame rate z pliku: " + videoFile.getName(), e);
            return Optional.empty();
        }
    }

    private boolean isFFprobeAvailable() {
        try {
            Process process = new ProcessBuilder("ffprobe", "-version").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private double extractFrameRateWithFFprobe(File videoFile) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "quiet",
                "-show_entries", "stream=r_frame_rate",
                "-select_streams", "v:0",
                "-of", "csv=p=0",
                videoFile.getAbsolutePath()
        );

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String output = reader.readLine();

            if (StringUtils.isNotBlank(output)) {
                String[] parts = StringUtils.split(StringUtils.trim(output), "/");
                if (parts.length == 2) {
                    double numerator = Double.parseDouble(parts[0]);
                    double denominator = Double.parseDouble(parts[1]);
                    return numerator / denominator;
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFprobe zakończył się błędem (kod: " + exitCode + ")");
        }

        throw new IOException("Nie udało się wydobyć frame rate z metadanych");
    }

    private Optional<FrameRate> findClosestFrameRate(double detectedFps) {
        FrameRate closestFrameRate = null;
        double smallestDifference = Double.MAX_VALUE;

        for (FrameRate frameRate : FrameRate.values()) {
            double difference = Math.abs(frameRate.getPreciseValue().doubleValue() - detectedFps);
            if (difference < smallestDifference) {
                smallestDifference = difference;
                closestFrameRate = frameRate;
            }
        }

        // Tylko jeśli różnica jest mniejsza niż 0.5 FPS
        if (smallestDifference < 0.5) {
            return Optional.of(closestFrameRate);
        }

        return Optional.empty();
    }
}
