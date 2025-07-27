package app

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

    private File createTempSrtFile(String fileName, String content) {
        def file = tempDir.resolve(fileName).toFile()
        file.text = content
        return file
    }
}
