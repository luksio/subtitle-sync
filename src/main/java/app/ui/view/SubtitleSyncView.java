package app.ui.view;

import app.model.FrameRate;

import java.io.File;
import java.util.Optional;

/**
 * View interface for Subtitle Sync UI operations.
 * Abstracts UI implementation from business logic (Presenter).
 * No Swing types in signatures - enables testing with mock implementations.
 */
public interface SubtitleSyncView {

    /**
     * Display selected subtitle file name in UI.
     *
     * @param fileName name of the selected subtitle file
     */
    void setSubtitleFileName(String fileName);

    /**
     * Update displayed offset value.
     *
     * @param offsetText formatted offset text (e.g., "5.0 s")
     */
    void setOffsetValue(String offsetText);

    /**
     * Set source frame rate in UI.
     *
     * @param frameRate source frame rate
     */
    void setFromFrameRate(FrameRate frameRate);

    /**
     * Set target frame rate in UI.
     *
     * @param frameRate target frame rate
     */
    void setToFrameRate(FrameRate frameRate);

    /**
     * Get currently selected subtitle file.
     *
     * @return current subtitle file, or null if none selected
     */
    File getCurrentSubtitleFile();

    /**
     * Get current offset value in seconds.
     *
     * @return offset in seconds
     */
    double getOffsetSeconds();

    /**
     * Get selected source frame rate.
     *
     * @return source frame rate
     */
    FrameRate getFromFrameRate();

    /**
     * Get selected target frame rate.
     *
     * @return target frame rate
     */
    FrameRate getToFrameRate();

    /**
     * Display error message to user.
     *
     * @param message error message
     */
    void showError(String message);

    /**
     * Display success message to user.
     *
     * @param message success message
     */
    void showSuccess(String message);

    /**
     * Set UI busy state (e.g., show wait cursor during processing).
     *
     * @param busy true to show busy state, false to restore normal state
     */
    void setBusy(boolean busy);

    /**
     * Show file chooser dialog for selecting subtitle file.
     *
     * @return selected file, or empty if user cancelled
     */
    Optional<File> chooseSubtitleFile();

    /**
     * Show file chooser dialog for selecting video file.
     *
     * @return selected file, or empty if user cancelled
     */
    Optional<File> chooseVideoFile();
}