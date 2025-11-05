package app.presenter

import app.model.FrameRate
import app.service.SubtitleService
import app.service.VideoMetadataService
import app.ui.view.SubtitleSyncView
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class SubtitleSyncPresenterSpec extends Specification {

    @TempDir
    Path tempDir

    SubtitleSyncView view
    SubtitleService subtitleService
    VideoMetadataService videoMetadataService
    SubtitleSyncPresenter presenter

    def setup() {
        view = Mock(SubtitleSyncView)
        subtitleService = Mock(SubtitleService)
        videoMetadataService = Mock(VideoMetadataService)
        presenter = new SubtitleSyncPresenter(view, subtitleService, videoMetadataService)
    }

    def 'should update view when subtitle file is selected'() {
        given: 'user selects a subtitle file'
            def testFile = Files.createFile(tempDir.resolve("test.srt")).toFile()
            view.chooseSubtitleFile() >> Optional.of(testFile)

        when: 'subtitle file selection is triggered'
            presenter.onSubtitleFileSelected()

        then: 'view displays the file name'
            1 * view.setSubtitleFileName("test.srt")
    }

    def 'should not update view when subtitle file selection is cancelled'() {
        given: 'user cancels file selection'
            view.chooseSubtitleFile() >> Optional.empty()

        when: 'subtitle file selection is triggered'
            presenter.onSubtitleFileSelected()

        then: 'view is not updated'
            0 * view.setSubtitleFileName(_)
    }

    def 'should show error when saving shifted subtitles without file selected'() {
        given: 'no subtitle file is selected'
            view.getCurrentSubtitleFile() >> null

        when: 'user attempts to save shifted subtitles'
            presenter.onSaveShiftedSubtitles()

        then: 'error message is shown'
            1 * view.showError("No subtitle file selected.")
    }

    def 'should save shifted subtitles and show success message'() {
        given: 'subtitle file is selected'
            def inputFile = Files.createFile(tempDir.resolve("input.srt")).toFile()
            def outputFile = tempDir.resolve("input_shifted.srt").toFile()
            view.getCurrentSubtitleFile() >> inputFile
            view.getOffsetSeconds() >> 5.0

        and: 'service processes the file successfully'
            subtitleService.createShiftedSubtitles(inputFile, 5.0) >> outputFile

        when: 'user saves shifted subtitles'
            presenter.onSaveShiftedSubtitles()

        then: 'success message is shown with output filename'
            1 * view.showSuccess("Shifted subtitles saved as:\ninput_shifted.srt")
    }

    def 'should show error when service fails to create shifted subtitles'() {
        given: 'subtitle file is selected'
            def inputFile = Files.createFile(tempDir.resolve("input.srt")).toFile()
            view.getCurrentSubtitleFile() >> inputFile
            view.getOffsetSeconds() >> 5.0

        and: 'service throws exception'
            subtitleService.createShiftedSubtitles(inputFile, 5.0) >> {
                throw new IOException("Disk full")
            }

        when: 'user attempts to save shifted subtitles'
            presenter.onSaveShiftedSubtitles()

        then: 'error message is shown'
            1 * view.showError("Failed to process file: Disk full")
    }

    def 'should show error when converting frame rate without file selected'() {
        given: 'no subtitle file is selected'
            view.getCurrentSubtitleFile() >> null

        when: 'user attempts frame rate conversion'
            presenter.onFrameRateConversion()

        then: 'error message is shown'
            1 * view.showError("No subtitle file selected.")
    }

    def 'should show error when source and target frame rates are identical'() {
        given: 'subtitle file is selected'
            def inputFile = Files.createFile(tempDir.resolve("input.srt")).toFile()
            view.getCurrentSubtitleFile() >> inputFile
            view.getFromFrameRate() >> FrameRate.FPS_25
            view.getToFrameRate() >> FrameRate.FPS_25

        when: 'user attempts conversion with identical frame rates'
            presenter.onFrameRateConversion()

        then: 'error message is shown'
            1 * view.showError("Source and target frame rate are identical.")
    }

    def 'should convert frame rate and show success message'() {
        given: 'subtitle file is selected with different frame rates'
            def inputFile = Files.createFile(tempDir.resolve("input.srt")).toFile()
            def outputFile = tempDir.resolve("input_25_fps_to_23_976_fps.srt").toFile()
            view.getCurrentSubtitleFile() >> inputFile
            view.getFromFrameRate() >> FrameRate.FPS_25
            view.getToFrameRate() >> FrameRate.FPS_23_976

        and: 'service processes conversion successfully'
            subtitleService.createFrameRateConvertedSubtitles(inputFile, FrameRate.FPS_25, FrameRate.FPS_23_976) >> outputFile

        when: 'user saves converted subtitles'
            presenter.onFrameRateConversion()

        then: 'success message is shown'
            1 * view.showSuccess("Converted subtitles saved as:\ninput_25_fps_to_23_976_fps.srt")
    }

    def 'should show error when frame rate conversion fails with IllegalArgumentException'() {
        given: 'subtitle file is selected'
            def inputFile = Files.createFile(tempDir.resolve("input.srt")).toFile()
            view.getCurrentSubtitleFile() >> inputFile
            view.getFromFrameRate() >> FrameRate.FPS_25
            view.getToFrameRate() >> FrameRate.FPS_23_976

        and: 'service throws IllegalArgumentException'
            subtitleService.createFrameRateConvertedSubtitles(inputFile, FrameRate.FPS_25, FrameRate.FPS_23_976) >> {
                throw new IllegalArgumentException("Invalid frame rate")
            }

        when: 'user attempts conversion'
            presenter.onFrameRateConversion()

        then: 'error message is shown'
            1 * view.showError("Parameter error: Invalid frame rate")
    }

    def 'should show error when frame rate conversion fails with IOException'() {
        given: 'subtitle file is selected'
            def inputFile = Files.createFile(tempDir.resolve("input.srt")).toFile()
            view.getCurrentSubtitleFile() >> inputFile
            view.getFromFrameRate() >> FrameRate.FPS_25
            view.getToFrameRate() >> FrameRate.FPS_23_976

        and: 'service throws IOException'
            subtitleService.createFrameRateConvertedSubtitles(inputFile, FrameRate.FPS_25, FrameRate.FPS_23_976) >> {
                throw new IOException("File not found")
            }

        when: 'user attempts conversion'
            presenter.onFrameRateConversion()

        then: 'error message is shown'
            1 * view.showError("Failed to process file: File not found")
    }

    def 'should detect frame rate from video and update view'() {
        given: 'user selects a video file'
            def videoFile = Files.createFile(tempDir.resolve("test.mp4")).toFile()
            view.chooseVideoFile() >> Optional.of(videoFile)

        and: 'video metadata service detects frame rate'
            videoMetadataService.detectFrameRate(videoFile) >> Optional.of(FrameRate.FPS_23_976)

        when: 'frame rate detection is triggered'
            presenter.onDetectFrameRateFromVideo()

        then: 'view shows busy cursor'
            1 * view.setBusy(true)

        and: 'detected frame rate is set in view'
            1 * view.setToFrameRate(FrameRate.FPS_23_976)

        and: 'success message is shown'
            1 * view.showSuccess("Frame rate detected: " + FrameRate.FPS_23_976.getNameWithDescription())

        and: 'busy cursor is cleared'
            1 * view.setBusy(false)
    }

    def 'should show error when frame rate detection fails'() {
        given: 'user selects a video file'
            def videoFile = Files.createFile(tempDir.resolve("test.mp4")).toFile()
            view.chooseVideoFile() >> Optional.of(videoFile)

        and: 'video metadata service cannot detect frame rate'
            videoMetadataService.detectFrameRate(videoFile) >> Optional.empty()

        when: 'frame rate detection is triggered'
            presenter.onDetectFrameRateFromVideo()

        then: 'view shows busy cursor'
            1 * view.setBusy(true)

        and: 'error message is shown with possible reasons'
            1 * view.showError({ String msg ->
                msg.contains("Failed to detect frame rate") &&
                msg.contains("Unsupported file format") &&
                msg.contains("Corrupted metadata")
            })

        and: 'busy cursor is cleared'
            1 * view.setBusy(false)
    }

    def 'should not detect frame rate when video selection is cancelled'() {
        given: 'user cancels video file selection'
            view.chooseVideoFile() >> Optional.empty()

        when: 'frame rate detection is triggered'
            presenter.onDetectFrameRateFromVideo()

        then: 'no interactions with video metadata service'
            0 * videoMetadataService.detectFrameRate(_)

        and: 'busy cursor is not shown'
            0 * view.setBusy(_)
    }

    def 'should update offset label when slider changes'() {
        given: 'current offset is 5.0 seconds'
            view.getOffsetSeconds() >> 5.0

        when: 'offset slider changes'
            presenter.onOffsetChanged()

        then: 'offset label is updated'
            1 * view.setOffsetValue("5.0 s")
    }

    def 'should update offset label with negative value'() {
        given: 'current offset is -10.0 seconds'
            view.getOffsetSeconds() >> -10.0

        when: 'offset slider changes'
            presenter.onOffsetChanged()

        then: 'offset label is updated with negative value'
            1 * view.setOffsetValue("-10.0 s")
    }
}