package app.service

import app.model.FrameRate
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class SubtitleServiceSpec extends Specification {

    @TempDir
    Path tempDir

    SubtitleService subtitleService = new SubtitleService()

    def 'should create shifted subtitles file with correct offset'() {
        given: 'input SRT file with various subtitles and dialogues'
            def inputContent = '''1
00:00:58,500 --> 00:01:01,200
First subtitle before minute

2
00:01:57,800 --> 00:02:00,300
- Hello! How are you?
- Great, thanks!

3
00:59:58,000 --> 01:00:02,500
Subtitle before hour
with multiple lines of text

4
01:00:10,750 --> 01:00:13,250
Last subtitle after hour
'''
        and: 'offset that will change minutes and hours'
            def offsetSeconds = 3.7

        and: 'expected output after shifting by 3.7 seconds'
            def expectedOutput = '''1
00:01:02,200 --> 00:01:04,900
First subtitle before minute

2
00:02:01,500 --> 00:02:04,000
- Hello! How are you?
- Great, thanks!

3
01:00:01,700 --> 01:00:06,200
Subtitle before hour
with multiple lines of text

4
01:00:14,450 --> 01:00:16,950
Last subtitle after hour

'''

        when: 'shifting subtitles by the specified offset'
            def inputFile = createTempSrtFile('test.srt', inputContent)
            def outputFile = subtitleService.createShiftedSubtitles(inputFile, offsetSeconds)

        then: 'output file is created with correct name'
            outputFile.exists()
            outputFile.name == 'test_shifted.srt'

        and: 'file content matches expected output exactly'
            outputFile.text == expectedOutput
    }

    def 'should handle negative offset that changes hours and minutes'() {
        given: 'input SRT file with subtitles at various times including early ones'
            def inputContent = '''1
01:05:30,250 --> 01:05:35,750
Subtitle after hour

2
01:00:02,100 --> 01:00:05,600
Subtitle just after hour

3
00:00:30,500 --> 00:00:33,000
Early subtitle that would go negative
'''
        and: 'negative offset of 1 minute and 5.5 seconds'
            def offsetSeconds = -65.5

        and: 'expected output after shifting back by 65.5 seconds'
            def expectedOutput = '''1
01:04:24,750 --> 01:04:30,250
Subtitle after hour

2
00:58:56,600 --> 00:59:00,100
Subtitle just after hour

3
00:00:00,000 --> 00:00:00,000
Early subtitle that would go negative

'''

        when: 'shifting subtitles by negative offset'
            def inputFile = createTempSrtFile('late.srt', inputContent)
            def outputFile = subtitleService.createShiftedSubtitles(inputFile, offsetSeconds)

        then: 'times are correctly adjusted with hour boundary crossing and negative values clamped to zero'
            outputFile.text == expectedOutput
    }

    def 'should preserve subtitle numbering and text formatting'() {
        given: 'input SRT file with various formatting'
            def inputContent = '''1
00:00:01,000 --> 00:00:03,000
<i>Italic text</i>

2
00:00:05,000 --> 00:00:07,000
Regular text
with new line

3
00:00:10,000 --> 00:00:12,000
UPPERCASE TEXT
'''
        and: 'simple 1 second offset'
            def offsetSeconds = 1.0

        and: 'expected output with preserved formatting and shifted times'
            def expectedOutput = '''1
00:00:02,000 --> 00:00:04,000
<i>Italic text</i>

2
00:00:06,000 --> 00:00:08,000
Regular text
with new line

3
00:00:11,000 --> 00:00:13,000
UPPERCASE TEXT

'''

        when: 'shifting subtitles'
            def inputFile = createTempSrtFile('formatted.srt', inputContent)
            def outputFile = subtitleService.createShiftedSubtitles(inputFile, offsetSeconds)

        then: 'formatting and numbering are preserved'
            outputFile.text == expectedOutput
    }

    def 'should handle file with BOM correctly'() {
        given: 'input SRT file with BOM (Byte Order Mark)'
            def inputContent = '\uFEFF1\n00:00:01,000 --> 00:00:03,000\nSubtitle with BOM\n'

        and: 'zero offset to test BOM removal only'
            def offsetSeconds = 0.0

        and: 'expected output without BOM'
            def expectedOutput = '''1
00:00:01,000 --> 00:00:03,000
Subtitle with BOM

'''

        when: 'processing file with BOM'
            def inputFile = createTempSrtFile('with_bom.srt', inputContent)
            def outputFile = subtitleService.createShiftedSubtitles(inputFile, offsetSeconds)

        then: 'BOM is removed and content is correct'
            outputFile.text == expectedOutput
            !outputFile.text.contains('\uFEFF')
    }

    def 'should throw IOException when input file does not exist'() {
        given: 'non-existent file'
            def nonExistentFile = new File(tempDir.toFile(), 'does_not_exist.srt')

        when: 'trying to process non-existent file'
            subtitleService.createShiftedSubtitles(nonExistentFile, 1.0)

        then: 'IOException is thrown'
            thrown(IOException)
    }

    def 'should generate correct output filename for different input names'() {
        given: 'minimal SRT content for filename testing'
            def inputContent = '1\n00:00:01,000 --> 00:00:03,000\nTest\n'

        and: 'zero offset for simplicity'
            def offsetSeconds = 0.0

        when: 'creating shifted subtitles'
            def inputFile = createTempSrtFile(inputFileName, inputContent)
            def outputFile = subtitleService.createShiftedSubtitles(inputFile, offsetSeconds)

        then: 'output filename is correct'
            outputFile.name == expectedOutputName

        where: 'testing various input filename patterns'
            inputFileName        | expectedOutputName
            'movie.srt'          | 'movie_shifted.srt'
            'film.with.dots.srt' | 'film.with.dots_shifted.srt'
            'noextension'        | 'noextension_shifted.srt'
            'multi.dot.file.srt' | 'multi.dot.file_shifted.srt'
    }

    def 'should handle zero offset correctly'() {
        given: 'input SRT file'
            def inputContent = '''1
00:01:30,500 --> 00:01:33,750
Test subtitle
'''
        and: 'zero offset (no change expected)'
            def offsetSeconds = 0.0

        and: 'expected output identical to input'
            def expectedOutput = '''1
00:01:30,500 --> 00:01:33,750
Test subtitle

'''

        when: 'applying zero offset'
            def inputFile = createTempSrtFile('zero_test.srt', inputContent)
            def outputFile = subtitleService.createShiftedSubtitles(inputFile, offsetSeconds)

        then: 'output is identical to input'
            outputFile.text == expectedOutput
    }

    // test frame rate conversion

    def 'should create frame rate converted subtitles file with correct timing'() {
        given: 'input SRT file with subtitles at various times'
            def inputContent = '''1
00:01:00,000 --> 00:01:04,000
First subtitle at 1 minute

2
00:02:30,500 --> 00:02:35,750
Second subtitle with fractional seconds

3
00:10:15,200 --> 00:10:20,800
Third subtitle at 10+ minutes
'''
        and: 'conversion from 25 FPS to 23.976 FPS (film to TV conversion)'
            def fromFrameRate = FrameRate.FPS_25
            def toFrameRate = FrameRate.FPS_23_976

        and: 'expected output after frame rate conversion (times should be longer)'
            def expectedOutput = '''1
00:01:02,562 --> 00:01:06,733
First subtitle at 1 minute

2
00:02:36,927 --> 00:02:42,401
Second subtitle with fractional seconds

3
00:10:41,474 --> 00:10:47,313
Third subtitle at 10+ minutes

'''

        when: 'converting frame rate'
            def inputFile = createTempSrtFile('test.srt', inputContent)
            def outputFile = subtitleService.createFrameRateConvertedSubtitles(inputFile, fromFrameRate, toFrameRate)

        then: 'output file is created with correct name'
            outputFile.exists()
            outputFile.name == 'test_25_fps_to_23_976_fps.srt'

        and: 'timing is correctly converted'
            outputFile.text == expectedOutput
    }

    def 'should handle frame rate conversion from slower to faster rate'() {
        given: 'input SRT file with multiple subtitles'
            def inputContent = '''1
00:01:00,000 --> 00:01:04,000
First subtitle at 1 minute

2
00:05:00,000 --> 00:05:03,000
Second subtitle at 5 minutes

3
00:10:30,500 --> 00:10:35,250
Third subtitle with fractional seconds
'''
        and: 'conversion from 23.976 FPS to 25 FPS (times should get shorter)'
            def fromFrameRate = FrameRate.FPS_23_976
            def toFrameRate = FrameRate.FPS_25

        and: 'expected output with shorter timing'
            def expectedOutput = '''1
00:00:57,542 --> 00:01:01,378
First subtitle at 1 minute

2
00:04:47,712 --> 00:04:50,589
Second subtitle at 5 minutes

3
00:10:04,674 --> 00:10:09,230
Third subtitle with fractional seconds

'''

        when: 'converting frame rate'
            def inputFile = createTempSrtFile('fast.srt', inputContent)
            def outputFile = subtitleService.createFrameRateConvertedSubtitles(inputFile, fromFrameRate, toFrameRate)

        then: 'timing is correctly shortened'
            outputFile.text == expectedOutput
            outputFile.name == 'fast_23_976_fps_to_25_fps.srt'
    }

    def 'should handle frame rate conversion with long movie duration'() {
        given: 'input SRT file with subtitles from long movie (2+ hours)'
            def inputContent = '''1
02:15:30,750 --> 02:15:35,250
Subtitle at 2h 15min mark

2
02:45:12,100 --> 02:45:18,900
Subtitle near end of long movie

3
03:02:45,500 --> 03:02:50,000
End credits subtitle
'''
        and: 'conversion from 25 FPS to 23.976 FPS (times should get longer)'
            def fromFrameRate = FrameRate.FPS_25
            def toFrameRate = FrameRate.FPS_23_976

        and: 'expected output with longer timing for 2+ hour content'
            def expectedOutput = '''1
02:21:18,009 --> 02:21:22,701
Subtitle at 2h 15min mark

2
02:52:15,439 --> 02:52:22,530
Subtitle near end of long movie

3
03:10:33,829 --> 03:10:38,521
End credits subtitle

'''

        when: 'converting frame rate for long content'
            def inputFile = createTempSrtFile('long_movie.srt', inputContent)
            def outputFile = subtitleService.createFrameRateConvertedSubtitles(inputFile, fromFrameRate, toFrameRate)

        then: 'timing is correctly converted for long duration'
            outputFile.text == expectedOutput
            outputFile.name == 'long_movie_25_fps_to_23_976_fps.srt'
    }

    def 'should handle extreme frame rate conversion'() {
        given: 'input SRT file with precise timing'
            def inputContent = '''1
00:00:01,000 --> 00:00:02,000
One second subtitle
'''
        and: 'conversion from 24 FPS to 60 FPS (2.5x faster)'
            def fromFrameRate = FrameRate.FPS_24
            def toFrameRate = FrameRate.FPS_60

        and: 'expected output with much shorter timing'
            def expectedOutput = '''1
00:00:00,400 --> 00:00:00,800
One second subtitle

'''

        when: 'converting with extreme ratio'
            def inputFile = createTempSrtFile('extreme.srt', inputContent)
            def outputFile = subtitleService.createFrameRateConvertedSubtitles(inputFile, fromFrameRate, toFrameRate)

        then: 'timing is correctly adjusted'
            outputFile.text == expectedOutput
    }

    def 'should throw IllegalArgumentException when frame rates are identical'() {
        given: 'input SRT file'
            def inputContent = '1\n00:00:01,000 --> 00:00:03,000\nTest\n'
            def inputFile = createTempSrtFile('same.srt', inputContent)

        and: 'identical source and target frame rates'
            def frameRate = FrameRate.FPS_25

        when: 'trying to convert between same frame rates'
            subtitleService.createFrameRateConvertedSubtitles(inputFile, frameRate, frameRate)

        then: 'IllegalArgumentException is thrown'
            def ex = thrown(IllegalArgumentException)
            ex.message == 'Źródłowy i docelowy frame rate są identyczne'
    }

    def 'should generate correct output filename for frame rate conversion'() {
        given: 'minimal SRT content'
            def inputContent = '1\n00:00:01,000 --> 00:00:03,000\nTest\n'

        when: 'converting frame rate'
            def inputFile = createTempSrtFile(inputFileName, inputContent)
            def outputFile = subtitleService.createFrameRateConvertedSubtitles(inputFile, fromFps, toFps)

        then: 'output filename is correct'
            outputFile.name == expectedOutputName

        where: 'testing various frame rate combinations'
            inputFileName | fromFps              | toFps               | expectedOutputName
            'movie.srt'   | FrameRate.FPS_24     | FrameRate.FPS_25    | 'movie_24_fps_to_25_fps.srt'
            'film.srt'    | FrameRate.FPS_23_976 | FrameRate.FPS_29_97 | 'film_23_976_fps_to_29_97_fps.srt'
            'tv.srt'      | FrameRate.FPS_30     | FrameRate.FPS_60    | 'tv_30_fps_to_60_fps.srt'
            'test'        | FrameRate.FPS_25     | FrameRate.FPS_50    | 'test_25_fps_to_50_fps.srt'
    }

    def 'should handle very short durations in frame rate conversion'() {
        given: 'input SRT file with very short subtitle'
            def inputContent = '''1
00:00:00,100 --> 00:00:00,200
Quick flash
'''
        and: 'conversion from fast to slow frame rate'
            def fromFrameRate = FrameRate.FPS_60
            def toFrameRate = FrameRate.FPS_24

        when: 'converting frame rate'
            def inputFile = createTempSrtFile('quick.srt', inputContent)
            def outputFile = subtitleService.createFrameRateConvertedSubtitles(inputFile, fromFrameRate, toFrameRate)

        then: 'file is created successfully'
            outputFile.exists()

        and: 'content is valid (times should be longer)'
            def content = outputFile.text
            content.contains('00:00:00,250 --> 00:00:00,500')
    }

    def 'should preserve text formatting during frame rate conversion'() {
        given: 'input SRT file with formatted text'
            def inputContent = '''1
00:01:00,000 --> 00:01:02,000
<i>Italic text</i>
with multiple lines

2
00:02:00,000 --> 00:02:03,000
UPPERCASE TEXT
'''
        and: 'simple frame rate conversion'
            def fromFrameRate = FrameRate.FPS_25
            def toFrameRate = FrameRate.FPS_24

        when: 'converting frame rate'
            def inputFile = createTempSrtFile('formatted.srt', inputContent)
            def outputFile = subtitleService.createFrameRateConvertedSubtitles(inputFile, fromFrameRate, toFrameRate)

        then: 'text formatting is preserved'
            def content = outputFile.text
            content.contains('<i>Italic text</i>')
            content.contains('with multiple lines')
            content.contains('UPPERCASE TEXT')

        and: 'timing is adjusted but text remains intact'
            content.contains('00:01:02,500 --> 00:01:04,583')
            content.contains('00:02:05,000 --> 00:02:08,125')
    }
    
    private File createTempSrtFile(String fileName, String content) {
        def file = tempDir.resolve(fileName).toFile()
        file.text = content
        return file
    }
}
