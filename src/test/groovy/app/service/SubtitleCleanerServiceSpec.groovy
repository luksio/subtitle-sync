package app.service

import app.model.SubtitleEntry
import spock.lang.PendingFeature
import spock.lang.Specification

import java.time.Duration

class SubtitleCleanerServiceSpec extends Specification {

    @PendingFeature
    def 'should remove sound descriptions in parentheses from line'() {
        given: 'subtitle with sound description in parentheses at the end'
            def entry = createEntry(2197, '01:37:49,660', '01:37:51,458', 'Ray! (CRYING)')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'sound description is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'Ray!'
    }

    @PendingFeature
    def 'should remove sound descriptions in parentheses from multi-line subtitle'() {
        given: 'subtitle with sound description on first line and dialog on second'
            def entry = createEntry(2248, '01:39:58,153', '01:40:01,786', '- (SOBBING)\n- MAJOR: Don\'t throw it away.')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'sound description line is removed, speaker prefix is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'Don\'t throw it away.'
    }

    @PendingFeature
    def 'should remove entire entry when it contains only sound descriptions'() {
        given: 'subtitle with only sound description'
            def entry = createEntry(2195, '01:37:44,080', '01:37:46,457', '(CROWD CHEERING)')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'entry is completely removed'
            result.isEmpty()
    }

    @PendingFeature
    def 'should remove entry with multiple sound description lines'() {
        given: 'subtitle with two lines of sound descriptions'
            def entry = createEntry(1458, '01:42:19,672', '01:42:22,772', '-(Gwen grunting)\n-(pained squealing)')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'entry is completely removed'
            result.isEmpty()
    }

    @PendingFeature
    def 'should remove entry with only music symbols'() {
        given: 'subtitle with only music symbols'
            def entry = createEntry(1461, '01:42:53,403', '01:42:55,607', '♪ ♪')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'entry is completely removed'
            result.isEmpty()
    }

    @PendingFeature
    def 'should remove sound description line but keep dialog line'() {
        given: 'subtitle with dialog and sound description on separate lines'
            def entry = createEntry(1332, '01:32:22,180', '01:32:24,538', '-No.\n-(sighs)')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'sound description line is removed, dialog remains without dash'
            result.size() == 1
            result[0].text() == 'No.'
    }

    @PendingFeature
    def 'should remove entry with italicized sound description'() {
        given: 'subtitle with sound description in italics tags'
            def entry = createEntry(1, '00:00:03,837', '00:00:06,114', '<i> (static crackling softly)</i>')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'entry is completely removed'
            result.isEmpty()
    }

    @PendingFeature
    def 'should remove speaker name prefix but keep dialog'() {
        given: 'subtitle with speaker name and action description'
            def entry = createEntry(27, '00:03:40,463', '00:03:41,954', 'STUDENTS (chanting):\n<i> Fight! Fight!</i>')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'speaker prefix is removed, dialog remains'
            result.size() == 1
            result[0].text() == '<i> Fight! Fight!</i>'
    }

    @PendingFeature
    def 'should keep song lyrics with music symbols'() {
        given: 'subtitle with song lyrics and music symbols'
            def entry = createEntry(2187, '02:29:53,402', '02:29:55,654', '♪ Pedal down and drive ♪')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'song lyrics are preserved'
            result.size() == 1
            result[0].text() == '♪ Pedal down and drive ♪'
    }

    @PendingFeature
    def 'should remove speaker name in square brackets'() {
        given: 'subtitle with speaker name in square brackets'
            def entry = createEntry(204, '02:08:51,182', '02:08:52,934', '[real Ed] <i>When a person\nbecomes frightened,</i>')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'speaker name is removed, dialog remains'
            result.size() == 1
            result[0].text() == '<i>When a person\nbecomes frightened,</i>'
    }

    @PendingFeature
    def 'should remove sound description in square brackets'() {
        given: 'subtitle with sound description in square brackets'
            def entry = createEntry(89, '01:58:57,840', '01:58:59,675', '[sobbing] They won\'t go away.')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'sound description is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'They won\'t go away.'
    }

    @PendingFeature
    def 'should remove speaker name with action description'() {
        given: 'subtitle with speaker name and action description'
            def entry = createEntry(38, '00:04:02,949', '00:04:04,884', 'STUDENT ON GROUND (panting):\nPlease, stop. Please.')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'speaker prefix is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'Please, stop. Please.'
    }

    @PendingFeature
    def 'should keep song title information'() {
        given: 'subtitle with song title and artist in italics'
            def entry = createEntry(93, '00:06:13,748', '00:06:15,813', '<i> ("Subways of Your Mind"</i>\n<i> by FEX playing)</i>')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'song information is preserved'
            result.size() == 1
            result[0].text() == '<i> ("Subways of Your Mind"</i>\n<i> by FEX playing)</i>'
    }

    @PendingFeature
    def 'should remove speaker name but keep dialog with dash'() {
        given: 'subtitle with dialog and speaker name'
            def entry = createEntry(67, '00:05:16,185', '00:05:17,788', '-Hi, Ernie.\n-ERNESTO: So, uh, tonight,')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'speaker name is removed, dialog with dash remains'
            result.size() == 1
            result[0].text() == '-Hi, Ernie.\n-So, uh, tonight,'
    }

    @PendingFeature
    def 'should remove sound description line from multi-line dialog'() {
        given: 'subtitle with sound description and dialog on separate lines'
            def entry = createEntry(32, '00:02:39,751', '00:02:42,260', '- (BRAKES SQUEALING)\n- ID, please.')

        when: 'removing SDH content'
            def result = SubtitleCleanerService.removeSDH([entry])

        then: 'sound description line is removed, dialog remains'
            result.size() == 1
            result[0].text() == 'ID, please.'
    }

    // Helper method to create SubtitleEntry from SRT-formatted timestamps
    private static SubtitleEntry createEntry(int index, String startTime, String endTime, String text) {
        return new SubtitleEntry(index, parseTime(startTime), parseTime(endTime), text)
    }

    private static Duration parseTime(String time) {
        def parts = time.split(':')
        def hours = parts[0] as int
        def minutes = parts[1] as int
        def secondsParts = parts[2].split(',')
        def seconds = secondsParts[0] as int
        def millis = secondsParts[1] as int

        return Duration.ofHours(hours)
                .plusMinutes(minutes)
                .plusSeconds(seconds)
                .plusMillis(millis)
    }
}
