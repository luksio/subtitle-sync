package app.service

import app.TestResourceUtils
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class SdhEdgeCasesSpec extends Specification {

    @TempDir
    Path tempDir

    private Map<Integer, String> cleanedByIndex(String fileName) {
        def file = TestResourceUtils.copySubtitleToTemp("sdh/${fileName}", tempDir)
        def entries = SubtitleParserService.parseFile(file)
        def cleaned = SubtitleCleanerService.removeSdh(entries)
        return cleaned.collectEntries { [(it.index()): it.text()] }
    }

    def 'speakers.srt — supported cases: #description'() {
        given:
            def results = cleanedByIndex('speakers.srt')

        expect:
            results[entryIndex] == expectedText

        where:
            description                                | entryIndex | expectedText
            'OFFICER #1: speaker with "#"'             | 1          | 'Stop right there'
            'DR. SMITH: speaker with "."'              | 2          | 'What happened?'
            '[JOSÉ]: trailing colon stripped'          | 3          | '¿Qué pasa?'
            'MÜLLER (shouting): non-ASCII speaker'     | 4          | 'Halt!'
            'MAN #2 (OFF-SCREEN): speaker with "#"'    | 5          | 'Over here!'
            '[GIRL, 5 YEARS OLD] uppercase bracket'    | 6          | 'I want ice cream'
            "CAPTAIN O'BRIEN: speaker with apostrophe" | 7          | 'Ready the ship'
            '[MAN 1 & MAN 2]: trailing colon stripped' | 8          | 'Together we stand'
            '[boy] lowercase bracket'                  | 9          | 'She sleepwalks sometimes.'
            '[woman on TV] lowercase + spaces'         | 10         | 'No, thanks.'
            '[man] single-word lowercase'              | 11         | 'You should tell your brother.'
            '[Wesker] mixed-case character name'       | 12         | 'Hey!'
            '[Jill] mixed-case character name'         | 13         | 'Come on, just through here.'
    }

    def 'music.srt — supported cases: #description'() {
        given:
            def results = cleanedByIndex('music.srt')

        expect:
            results[entryIndex] == expectedText

        where:
            description                                                      | entryIndex | expectedText
            'only music symbols → entry removed'                             | 1          | null
            'lyrics with music symbols preserved'                            | 2          | "♪ I'm so tired of workin' every day ♪"
            '(Music playing in background) — entry removed'                  | 5          | null
            'sound desc line dropped, dialog line kept'                      | 6          | 'I love this song'
            'song info in parens kept'                                       | 7          | '(Music: "Don\'t Stop Me Now" playing)'
            'song info in parens with italics kept'                          | 8          | '<i>(music: "Sweet Child O\' Mine" by Guns N\' Roses playing)</i>'
            '<i>♪ "Yesterday" by The Beatles playing ♪</i> kept'             | 9          | '<i>♪ "Yesterday" by The Beatles playing ♪</i>'
            'song info in brackets kept'                                     | 10         | '["My Favourite Game" playing on stereo]'
            '[music: "..." playing] song info in brackets kept'              | 11         | '[music: "Bohemian Rhapsody" playing]'
            'sound desc on lyric line cleaned'                               | 3          | '♪ Na na na ♪'
            'humming sound desc on lyric line cleaned'                       | 4          | '♪ La la la ♪'
            'plain dialog with " by " trigger word'                          | 12         | 'I was attacked by a bear'
            'plain dialog with "playing" trigger word'                       | 13         | "They're playing games"
            'plain dialog with quotes'                                       | 14         | 'He "borrowed" my car'
            'lyrics + sound desc on two dashed lines'                        | 15         | '♪ Do re mi ♪'
    }

    def 'mixed.srt — supported cases: #description'() {
        given:
            def results = cleanedByIndex('mixed.srt')

        expect:
            results[entryIndex] == expectedText

        where:
            description                                                | entryIndex | expectedText
            'double speaker [GIRL] MARY: both removed'                 | 1          | 'Hello'
            'two bracket sound descs at start removed'                 | 2          | 'Hello there'
            'JOHN (quietly): full speaker with parens removed'         | 3          | "I can't believe it"
            'multi-line bracket speakers with dashes'                  | 4          | '-Hey there\n-What\'s up?'
            '(music playing) ♪ La la la ♪ — sound desc removed'        | 5          | '♪ La la la ♪'
            'mid-sentence (coughs) paren removed'                      | 6          | "I can't finish this sentence"
            '<i>[NARRATOR] (softly) ...</i> — speaker inside italics'  | 7          | '<i>Once upon a time</i>'
            'multi-line with second line being only-SDH'               | 8          | 'Morning'
            'mid-sentence [chuckles] removed'                          | 9          | 'Hey, whoa.'
            'pure SDH line on multi-line dialog removed'               | 10         | 'Hello?'
            'speaker on line 1 + pure SDH line 2 → line 1 cleaned'     | 11         | 'Come on, just through here.'
            '(sighs) pure paren entry removed'                         | 12         | null
            '(John sighs) pure paren entry removed'                    | 13         | null
    }

    def 'real-world.srt — supported cases: #description'() {
        given:
            def results = cleanedByIndex('real-world.srt')

        expect:
            results[entryIndex] == expectedText

        where:
            description                                                       | entryIndex | expectedText
            '[thunder rumbling] removed'                                      | 1          | null
            '<i>[eerie music playing]</i> removed'                            | 2          | null
            'two bracket sound descs only → entry removed'                    | 3          | null
            '-(GROANS) on line 1, dialog on line 2 → only dialog kept'        | 4          | 'You okay?'
            '[glass shatters] dialog'                                         | 5          | 'What was that?'
            'DISPATCHER (over radio): full speaker'                           | 6          | 'Unit 5, respond'
            '[in Spanish] bracket speaker'                                    | 7          | 'Buenos días'
            '-[clears throat] + dialog → only dialog kept'                    | 8          | 'As I was saying...'
            'two pure paren lines → entry removed'                            | 9          | null
            '[WOMAN ON TV] uppercase bracket speaker'                         | 10         | "Today's forecast shows..."
            '[speaking indistinctly] pure SDH'                                | 12         | null
            '-(GASPS) Oh my God! / -(SCREAMS)'                                | 13         | 'Oh my God!'
            '[ANNOUNCER] uppercase bracket speaker'                           | 14         | 'And the winner is...'
            '<i>(wind howling)</i> on line 1 + dialog line 2'                 | 15         | "It's so cold"
            '(BREATHING HEAVILY) + two dashed dialog lines'                   | 16         | "-Run!\n-I'm trying!"
            '♪♪ (upbeat jazz playing) — decorative ♪ entry removed'           | 11         | null
            '[boy] speaker + multi-line wrap'                                 | 17         | 'She sleepwalks sometimes\nsince our parents died.'
            'mid-line [chuckles] removed + bracket song info kept'            | 18         | '-Nobody, that\'s who.\n-["My Favourite Game" playing on stereo]'
            'three-dash dialog'                                               | 19         | '-First person speaking\n-Second person responds\n-Third person adds comment'
    }

    def 'malformed.srt — current behaviour: #description'() {
        given:
            def results = cleanedByIndex('malformed.srt')

        expect:
            results[entryIndex] == expectedText

        where:
            description                                          | entryIndex | expectedText
            '( CRYING ) with extra spaces → removed'             | 1          | null
            '[door opens — missing close, left as-is'            | 2          | '[door opens'
            '(MUSIC ♪)) extra close paren → removed'             | 3          | null
            '[ LOUD BANG  ] with extra spaces → removed'         | 4          | null
            'JOHN  (whispering):  Hello double spaces'           | 5          | 'Hello'
            '[NARRATOR  + URL, missing close → left as-is'       | 6          | '[NARRATOR  Visit www.example.com'
    }
}
