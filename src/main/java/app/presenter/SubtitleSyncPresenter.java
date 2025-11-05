package app.presenter;

import app.model.FrameRate;
import app.service.SubtitleService;
import app.service.VideoMetadataService;
import app.ui.view.SubtitleSyncView;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

@Log
@RequiredArgsConstructor
public class SubtitleSyncPresenter {

    private final SubtitleSyncView view;
    private final SubtitleService subtitleService;
    private final VideoMetadataService videoMetadataService;

    public void onSubtitleFileSelected() {
        view.chooseSubtitleFile().ifPresent(file ->
                view.setSubtitleFileName(file.getName())
        );
    }

    public void onSaveShiftedSubtitles() {
        File subtitleFile = view.getCurrentSubtitleFile();
        if (subtitleFile == null) {
            view.showError("No subtitle file selected.");
            return;
        }

        try {
            double offsetSeconds = view.getOffsetSeconds();
            File outputFile = subtitleService.createShiftedSubtitles(subtitleFile, offsetSeconds);
            view.showSuccess("Shifted subtitles saved as:\n" + outputFile.getName());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Failed to process file while shifting subtitles: " + subtitleFile, ex);
            view.showError("Failed to process file: " + ex.getMessage());
        }
    }

    public void onFrameRateConversion() {
        File subtitleFile = view.getCurrentSubtitleFile();
        if (subtitleFile == null) {
            view.showError("No subtitle file selected.");
            return;
        }

        FrameRate fromFrameRate = view.getFromFrameRate();
        FrameRate toFrameRate = view.getToFrameRate();

        if (fromFrameRate == toFrameRate) {
            view.showError("Source and target frame rate are identical.");
            return;
        }

        try {
            File outputFile = subtitleService.createFrameRateConvertedSubtitles(
                    subtitleFile, fromFrameRate, toFrameRate);
            view.showSuccess("Converted subtitles saved as:\n" + outputFile.getName());
        } catch (IllegalArgumentException ex) {
            log.log(Level.WARNING, "Parameter error during frame rate conversion for file: " + subtitleFile, ex);
            view.showError("Parameter error: " + ex.getMessage());
        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO error during frame rate conversion for file: " + subtitleFile, ex);
            view.showError("Failed to process file: " + ex.getMessage());
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Unexpected error during frame rate conversion for file: " + subtitleFile, ex);
            view.showError("An unexpected error occurred: " + ex.getMessage());
        }
    }

    public void onDetectFrameRateFromVideo() {
        view.chooseVideoFile().ifPresent(videoFile -> {
            view.setBusy(true);
            try {
                Optional<FrameRate> detectedFrameRate = videoMetadataService.detectFrameRate(videoFile);

                if (detectedFrameRate.isPresent()) {
                    view.setToFrameRate(detectedFrameRate.get());
                    view.showSuccess("Frame rate detected: " +
                            detectedFrameRate.get().getNameWithDescription());
                } else {
                    view.showError("Failed to detect frame rate from video file.\n" +
                            "Possible reasons:\n" +
                            "- Unsupported file format\n" +
                            "- Corrupted metadata\n" +
                            "- Missing frame rate information in file");
                }
            } finally {
                view.setBusy(false);
            }
        });
    }

    public void onOffsetChanged() {
        view.setOffsetValue(String.format("%.1f s", view.getOffsetSeconds()));
    }
}
