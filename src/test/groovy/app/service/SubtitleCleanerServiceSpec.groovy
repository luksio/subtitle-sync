package app.service

import app.util.TestFileUtils
import spock.lang.PendingFeature
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class SubtitleCleanerServiceSpec extends Specification {

    @TempDir
    Path tempDir

    @PendingFeature
    def 'should remove sound descriptions in parentheses from line'() {
        given: 'subtitle with sound description in parentheses at the end'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''2197
01:37:49,660 --> 01:37:51,458
Ray! (CRYING)''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'sound description is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'Ray!'
    }

    @PendingFeature
    def 'should remove sound descriptions in parentheses from multi-line subtitle'() {
        given: 'subtitle with sound description on first line and dialog on second'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''2248
01:39:58,153 --> 01:40:01,786
- (SOBBING)
- MAJOR: Don't throw it away.''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'sound description line is removed, speaker prefix is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'Don\'t throw it away.'
    }

    @PendingFeature
    def 'should remove entire entry when it contains only sound descriptions'() {
        given: 'subtitle with only sound description'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''2195
01:37:44,080 --> 01:37:46,457
(CROWD CHEERING)''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'entry is completely removed'
            result.isEmpty()
    }

    @PendingFeature
    def 'should remove entry with multiple sound description lines'() {
        given: 'subtitle with two lines of sound descriptions'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''1458
01:42:19,672 --> 01:42:22,772
-(Gwen grunting)
-(pained squealing)''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'entry is completely removed'
            result.isEmpty()
    }

    @PendingFeature
    def 'should remove entry with only music symbols'() {
        given: 'subtitle with only music symbols'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''1461
01:42:53,403 --> 01:42:55,607
♪ ♪''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'entry is completely removed'
            result.isEmpty()
    }

    @PendingFeature
    def 'should remove sound description line but keep dialog line'() {
        given: 'subtitle with dialog and sound description on separate lines'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''1332
01:32:22,180 --> 01:32:24,538
-No.
-(sighs)''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'sound description line is removed, dialog remains without dash'
            result.size() == 1
            result[0].text() == 'No.'
    }

    @PendingFeature
    def 'should remove entry with italicized sound description'() {
        given: 'subtitle with sound description in italics tags'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''1
00:00:03,837 --> 00:00:06,114
<i> (static crackling softly)</i>''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'entry is completely removed'
            result.isEmpty()
    }

    @PendingFeature
    def 'should remove speaker name prefix but keep dialog'() {
        given: 'subtitle with speaker name and action description'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''27
00:03:40,463 --> 00:03:41,954
STUDENTS (chanting):
<i> Fight! Fight!</i>''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'speaker prefix is removed, dialog remains'
            result.size() == 1
            result[0].text() == '<i> Fight! Fight!</i>'
    }

    @PendingFeature
    def 'should keep song lyrics with music symbols'() {
        given: 'subtitle with song lyrics and music symbols'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''2187
02:29:53,402 --> 02:29:55,654
♪ Pedal down and drive ♪''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'song lyrics are preserved'
            result.size() == 1
            result[0].text() == '♪ Pedal down and drive ♪'
    }

    @PendingFeature
    def 'should remove speaker name in square brackets'() {
        given: 'subtitle with speaker name in square brackets'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''204
02:08:51,182 --> 02:08:52,934
[real Ed] <i>When a person
becomes frightened,</i>''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'speaker name is removed, dialog remains'
            result.size() == 1
            result[0].text() == '<i>When a person\nbecomes frightened,</i>'
    }

    @PendingFeature
    def 'should remove sound description in square brackets'() {
        given: 'subtitle with sound description in square brackets'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''89
01:58:57,840 --> 01:58:59,675
[sobbing] They won't go away.''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'sound description is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'They won\'t go away.'
    }

    @PendingFeature
    def 'should remove speaker name with action description'() {
        given: 'subtitle with speaker name and action description'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''38
00:04:02,949 --> 00:04:04,884
STUDENT ON GROUND (panting):
Please, stop. Please.''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'speaker prefix is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'Please, stop. Please.'
    }

    @PendingFeature
    def 'should keep song title information'() {
        given: 'subtitle with song title and artist in italics'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''93
00:06:13,748 --> 00:06:15,813
<i> ("Subways of Your Mind"</i>
<i> by FEX playing)</i>''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'song information is preserved'
            result.size() == 1
            result[0].text() == '<i> ("Subways of Your Mind"</i>\n<i> by FEX playing)</i>'
    }

    @PendingFeature
    def 'should remove speaker name but keep dialog with dash'() {
        given: 'subtitle with dialog and speaker name'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''67
00:05:16,185 --> 00:05:17,788
-Hi, Ernie.
-ERNESTO: So, uh, tonight,''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'speaker name is removed, dialog with dash remains'
            result.size() == 1
            result[0].text() == '-Hi, Ernie.\n-So, uh, tonight,'
    }

    @PendingFeature
    def 'should remove sound description line from multi-line dialog'() {
        given: 'subtitle with sound description and dialog on separate lines'
            def entries = TestFileUtils.parseTestSrt(tempDir, '''32
00:02:39,751 --> 00:02:42,260
- (BRAKES SQUEALING)
- ID, please.''')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH(entries)

        then: 'sound description line is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'ID, please.'
    }

    @PendingFeature
    def 'should remove entries containing URLs'() {
        given: 'subtitle entry containing URL'
            def entries = TestFileUtils.parseTestSrt(tempDir, """1
00:00:01,000 --> 00:00:03,000
${spamText}""")

        when: 'removing spam content'
            def result = SubtitleCleanerService.removeSpam(entries)

        then: 'entry is completely removed'
            result.isEmpty()

        where:
            spamText << [
                    'Downloaded from www.opensubtitles.org',
                    'Subtitles by http://addic7ed.com',
                    'https://subscene.com - best subtitles'
            ]
    }
}
