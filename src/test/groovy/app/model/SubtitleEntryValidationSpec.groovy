package app.model

import app.exception.InvalidSubtitleException
import spock.lang.Specification

import java.time.Duration

class SubtitleEntryValidationSpec extends Specification {

    def 'should throw InvalidSubtitleException for invalid index'() {
        when: 'creating subtitle with invalid index'
            new SubtitleEntry(index, Duration.ofSeconds(10), Duration.ofSeconds(15), 'Some text')

        then: 'exception is thrown with proper message'
            InvalidSubtitleException ex = thrown()
            ex.message == "Subtitle #${index}: Subtitle index must be positive, got: ${index} [start: 00:00:10,000, end: 00:00:15,000] Text: \"Some text\""

        where:
            index << [0, -1, -5]
    }

    def 'should throw InvalidSubtitleException for null start time'() {
        when: 'creating subtitle with null start time'
            new SubtitleEntry(1, null, Duration.ofSeconds(15), 'Test text')

        then: 'exception is thrown with proper message'
            InvalidSubtitleException ex = thrown()
            ex.message == 'Subtitle #1: Start time cannot be null [end: 00:00:15,000] Text: "Test text"'
    }

    def 'should throw InvalidSubtitleException for null end time'() {
        when: 'creating subtitle with null end time'
            new SubtitleEntry(2, Duration.ofSeconds(10), null, 'Another text')

        then: 'exception is thrown with proper message'
            InvalidSubtitleException ex = thrown()
            ex.message == 'Subtitle #2: End time cannot be null [start: 00:00:10,000] Text: "Another text"'
    }

    def 'should throw InvalidSubtitleException for negative start time'() {
        when: 'creating subtitle with negative start time'
            new SubtitleEntry(3, Duration.ofSeconds(-5), Duration.ofSeconds(15), 'Negative time text')

        then: 'exception is thrown with proper message'
            InvalidSubtitleException ex = thrown()
            ex.message == 'Subtitle #3: Start time cannot be negative [start: 00:00:-5,000, end: 00:00:15,000] Text: "Negative time text"'
    }

    def 'should throw InvalidSubtitleException when end time is before or equal to start time'() {
        when: 'creating subtitle with invalid time range'
            new SubtitleEntry(4, startTime, endTime, 'Invalid time range')

        then: 'exception is thrown with proper message'
            InvalidSubtitleException ex = thrown()
            ex.message.contains('Subtitle #4: End time must be after start time')
            ex.message.contains('Text: "Invalid time range"')

        where:
            startTime              | endTime
            Duration.ofSeconds(10) | Duration.ofSeconds(10)  // equal times
            Duration.ofSeconds(15) | Duration.ofSeconds(10)  // end before start
            Duration.ofSeconds(30) | Duration.ofSeconds(5)   // significantly before
    }

    def 'should throw InvalidSubtitleException for blank text'() {
        when: 'creating subtitle with blank text'
            new SubtitleEntry(5, Duration.ofSeconds(10), Duration.ofSeconds(15), text)

        then: 'exception is thrown with proper message'
            InvalidSubtitleException ex = thrown()
            ex.message.contains('Subtitle #5: Subtitle text cannot be empty or blank')
            ex.message.contains('[start: 00:00:10,000, end: 00:00:15,000]')

        where:
            text << [null, '', '   ', '\t', '\n', '  \t\n  ']
    }

    def 'should truncate long text in error message'() {
        given: 'very long subtitle text'
            def longText = 'This is a very long subtitle text that should be truncated in the error message because it exceeds the 50 character limit for display purposes'

        when: 'creating subtitle with invalid index and long text'
            new SubtitleEntry(-1, Duration.ofSeconds(10), Duration.ofSeconds(15), longText)

        then: 'exception message contains truncated text'
            InvalidSubtitleException ex = thrown()
            ex.message == 'Subtitle #-1: Subtitle index must be positive, got: -1 [start: 00:00:10,000, end: 00:00:15,000] Text: "This is a very long subtitle text that should b..."'
    }

    def 'should handle newlines in text preview'() {
        given: 'subtitle text with newlines'
            def textWithNewlines = 'Line 1\nLine 2\nLine 3'

        when: 'creating subtitle with invalid index'
            new SubtitleEntry(0, Duration.ofSeconds(10), Duration.ofSeconds(15), textWithNewlines)

        then: 'exception message contains escaped newlines'
            InvalidSubtitleException ex = thrown()
            ex.message == 'Subtitle #0: Subtitle index must be positive, got: 0 [start: 00:00:10,000, end: 00:00:15,000] Text: "Line 1\\nLine 2\\nLine 3"'
    }

    def 'should parse valid subtitle correctly'() {
        when: 'parsing valid subtitle'
            def entry = SubtitleEntry.parse(1, '00:01:30,500 --> 00:01:35,200', 'Hello, world!')

        then: 'subtitle is created successfully'
            entry.index == 1
            entry.start == Duration.ofHours(0).plusMinutes(1).plusSeconds(30).plusMillis(500)
            entry.end == Duration.ofHours(0).plusMinutes(1).plusSeconds(35).plusMillis(200)
            entry.text == 'Hello, world!'
    }

    def 'should throw InvalidSubtitleException for empty timeline in parse'() {
        when: 'parsing subtitle with empty timeline'
            SubtitleEntry.parse(6, timeline, 'Some text')

        then: 'exception is thrown with proper message'
            InvalidSubtitleException ex = thrown()
            ex.message == 'Subtitle #6: Timeline cannot be empty Text: "Some text"'

        where:
            timeline << [null, '', '   ', '\t\n']
    }

    def 'should throw InvalidSubtitleException for invalid timeline format'() {
        when: 'parsing subtitle with invalid timeline format'
            SubtitleEntry.parse(7, 'invalid timeline format', 'Test text')

        then: 'exception is thrown with proper message'
            InvalidSubtitleException ex = thrown()
            ex.message == 'Subtitle #7: Invalid timeline format: invalid timeline format Text: "Test text"'
    }

    def 'should throw InvalidSubtitleException for missing end time in timeline'() {
        when: 'parsing subtitle with incomplete timeline'
            SubtitleEntry.parse(8, '00:01:30,500 -->', 'Incomplete timeline')

        then: 'exception is thrown with proper message'
            InvalidSubtitleException ex = thrown()
            ex.message == 'Subtitle #8: Invalid timeline format - missing end time: 00:01:30,500 --> [start: 00:01:30,500] Text: "Incomplete timeline"'
    }

    def 'should throw InvalidSubtitleException for malformed time in timeline'() {
        when: 'parsing subtitle with malformed time'
            SubtitleEntry.parse(9, '00:XX:30,500 --> 00:01:35,200', 'Bad time format')

        then: 'exception is thrown'
            InvalidSubtitleException ex = thrown()
            ex.message == 'Subtitle #9: Invalid timeline format - missing end time: 00:XX:30,500 --> 00:01:35,200 [start: 00:01:35,200] Text: "Bad time format"'
    }

    def 'should format subtitle correctly in SRT block'() {
        given: 'valid subtitle entry'
            def entry = new SubtitleEntry(42,
                    Duration.ofHours(1).plusMinutes(23).plusSeconds(45).plusMillis(678),
                    Duration.ofHours(1).plusMinutes(23).plusSeconds(50).plusMillis(123),
                    'Multi-line\nsubtitle text')

        when: 'converting to SRT block'
            def srtBlock = entry.toSrtBlock()

        then: 'SRT format is correct'
            srtBlock == '42\n01:23:45,678 --> 01:23:50,123\nMulti-line\nsubtitle text'
    }

    def 'should handle edge case durations correctly in error messages'() {
        when: 'creating subtitle with zero duration'
            new SubtitleEntry(10, Duration.ZERO, Duration.ZERO, 'Zero duration')

        then: 'exception message contains properly formatted zero duration'
            InvalidSubtitleException ex = thrown()
            ex.message.contains('[start: 00:00:00,000, end: 00:00:00,000]')
    }

    def 'should handle shift operations correctly'() {
        given: 'valid subtitle entry'
            def entry = new SubtitleEntry(1, Duration.ofSeconds(10), Duration.ofSeconds(15), 'Test subtitle')

        when: 'shifting by positive offset'
            def shifted = entry.shiftBySeconds(5.5)

        then: 'times are correctly shifted'
            shifted.index == 1
            shifted.start == Duration.ofSeconds(15).plusMillis(500)
            shifted.end == Duration.ofSeconds(20).plusMillis(500)
            shifted.text == 'Test subtitle'
    }

    def 'should clamp negative time to zero during shift'() {
        given: 'subtitle with small start time'
            def startTime = 2
            def negativeOffset = -4.0
            def entry = new SubtitleEntry(1, Duration.ofSeconds(startTime), Duration.ofSeconds(5), 'Early subtitle')

        when: 'shifting by larger negative offset'
            def shifted = entry.shiftBySeconds(negativeOffset)

        then: 'negative start time is clamped to zero'
            startTime + negativeOffset < 0
            shifted.start == Duration.ZERO
            shifted.end == Duration.ofSeconds(1)
    }

    def 'should maintain subtitle properties during frame rate conversion'() {
        given: 'valid subtitle entry'
            def entry = new SubtitleEntry(1, Duration.ofSeconds(60), Duration.ofSeconds(65), 'Frame rate test')
            def conversionRatio = new BigDecimal('0.8')

        when: 'converting frame rate'
            def converted = entry.convertFrameRate(conversionRatio)

        then: 'index and text are preserved, times are scaled'
            converted.index == 1
            converted.text == 'Frame rate test'
            converted.start == Duration.ofSeconds(48) // 60 * 0.8
            converted.end == Duration.ofSeconds(52)   // 65 * 0.8
    }
}
