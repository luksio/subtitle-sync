package app.util

import app.TestResourceUtils
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.charset.Charset
import java.nio.file.Path

class CharsetDetectorSpec extends Specification {

    @TempDir
    Path tempDir

    def "verify encoding is detected: #actualDetection from #subtitleFileName"(String actualDetection, String subtitleFileName) {
        given:
            def inputFile = TestResourceUtils.copySubtitleToTemp(subtitleFileName, tempDir)

        when:
            def detectedCharset = CharsetDetector.detectCharset(inputFile.toPath())

        then:
            if (actualDetection == null) {
                !detectedCharset.isDefined()
            } else {
                detectedCharset.isDefined()
                detectedCharset.get() == Charset.forName(actualDetection)
            }


        where: 'testing charset detection - reflects actual juniversalchardet behavior (even if incorrect)'
            actualDetection | subtitleFileName                   | comment
            'UTF-8'         | 'multilingual_utf8.srt'            | 'Polish + French + Spanish + German - detected correctly'
            null            | 'central_european_windows1250.srt' | 'ISSUE: Windows-1250 NOT detected (returns None)'
            'windows-1252'  | 'central_european_iso88592.srt'    | 'ISSUE: ISO-8859-2 wrongly detected as Windows-1252'
            'windows-1252'  | 'western_european_windows1252.srt' | 'Windows-1252 with typography'
            'windows-1252'  | 'western_european_iso88591.srt'    | 'ISSUE: ISO-8859-1 wrongly detected as Windows-1252'
    }

    def 'should return fallback charset when detection fails'() {
        given:
            def fallbackCharset = Charset.forName('KOI8-R')
            def inputFile = TestResourceUtils.copySubtitleToTemp('central_european_windows1250.srt', tempDir)

        when:
            def detectedCharset = CharsetDetector.detectCharsetWithFallback(inputFile.toPath(), fallbackCharset)

        then:
            detectedCharset == fallbackCharset
    }
}
