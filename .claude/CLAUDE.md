# Subtitle Sync — project guide

Java 24 Swing desktop app for SRT subtitle files. Features: time-offset shifting, frame-rate conversion (with ffprobe detection), SDH/spam cleaning with an audit log.

## Build & run

- Build: Maven 3.14+ via Makefile wrapper. `make help` lists targets.
- Run: `make run`. Tests: `mvn test`, single class: `mvn test -Dtest=ClassName`.
- Main class: `app.SubtitleSyncApp`.
- **External requirement**: `ffprobe` on PATH (only needed for frame-rate detection from video files; the rest works without it).

## Do NOT launch the app to verify changes

User verifies UI manually after each change. Don't `make run` or otherwise launch the app yourself, even for "smoke testing".

## Package layout

```
app/
├── SubtitleSyncApp                  # main()
├── presenter/SubtitleSyncPresenter  # all UI logic, event handling
├── ui/
│   ├── SubtitleSyncPanel            # JFrame content, implements SubtitleSyncView
│   ├── FileChooserHelper            # wraps java.awt.FileDialog (native OS picker)
│   └── view/SubtitleSyncView        # view interface, no Swing types
├── service/
│   ├── SubtitleParserService        # SRT → List<SubtitleEntry>
│   ├── SubtitleService              # file-level operations (shift, convert, clean)
│   ├── SubtitleCleanerService       # removeSdh / removeSpam on List<SubtitleEntry>
│   ├── SdhPatternMatcher            # all SDH regexes + cleanLine
│   ├── SubtitleChanges              # diff record (removed/modified/unchanged)
│   ├── SubtitleChangesLogWriter     # renders the *_changes.log file
│   ├── CleanResult                  # output of createCleanedSubtitles
│   └── VideoMetadataService         # ffprobe integration
├── model/
│   ├── SubtitleEntry (record)       # immutable, Duration-based timing, compact-constructor validation
│   └── FrameRate (enum)             # supported FPS with BigDecimal precision
├── util/
│   ├── CharsetDetector              # juniversalchardet wrapper, returns Vavr Option
│   └── UserPreferences              # persistent settings via java.util.prefs.Preferences
└── exception/InvalidSubtitleException
```

## MVP architecture (hard rules)

1. Services have **zero** Swing/AWT imports — keeps them headless-testable.
2. Presenters orchestrate; they own service references and call view methods.
3. Views are interfaces (`SubtitleSyncView`); the Swing panel is one implementation. Presenter tests use mock views.
4. Swing listeners delegate straight to presenter methods — no business logic in the panel.
5. Constructor injection only (services → presenter → view).

## Subtitle cleaning pipeline

```
createCleanedSubtitles(file, removeSdh, removeSpam)
  → SubtitleParserService.parseFile
  → SubtitleCleanerService.removeSdh (line-by-line via SdhPatternMatcher)
  → SubtitleCleanerService.removeSpam (URL filter)
  → diff against original → SubtitleChanges
  → write SRT (only if anything actually changed)
  → SubtitleChangesLogWriter writes *_changes.log next to output
```

Output naming:
- `_shifted.srt` (time offset)
- `_<from>_to_<to>.srt` (frame rate)
- `_cleaned.srt` / `_no_sdh.srt` / `_no_spam.srt` (cleaning, depending on which options were on)
- `*_changes.log` always written alongside cleaning output, even when no changes happened

## Precision

- `java.time.Duration` for timing (nanosecond internally; ms in SRT output).
- `BigDecimal` for frame-rate conversion ratios (10 decimal places, HALF_UP).

## Encoding

- Auto-detected via juniversalchardet. Two entry points on `CharsetDetector`: `detectCharset` (Vavr `Option`) and `detectCharsetWithFallback` (defaults to windows-1250, the most common Central European subtitle encoding).
- Input read in detected encoding, output always UTF-8.
- Parser handles BOM and is lenient with whitespace and blank lines.

## SRT format reminder

```
index
HH:MM:SS,mmm --> HH:MM:SS,mmm
text (may be multi-line)

```

Multi-line text is preserved through all operations.

## Testing

- Spock 2.4-M6 + Groovy 4.0.28, Given-When-Then style.
- Tests live in `src/test/groovy/app/`, mirroring main packages.
- Fixtures in `src/test/resources/subtitles/`:
  - Top level: charset detection samples (`central_european_*`, `western_european_*`, `multilingual_utf8`).
  - `sdh/`: SDH edge-case fixtures grouped by concern (`speakers.srt`, `music.srt`, `mixed.srt`, `real-world.srt`, `malformed.srt`). Driven by `SdhEdgeCasesSpec` with parameterized rows.
- `@PendingFeature(reason = "...")` marks tests that assert the *intended* behaviour for a known bug. When a fix lands, the iteration starts passing → Spock fails the build with "passes unexpectedly" → move that row from the pending method to the supported method in the same spec. This is the project's bug-tracking convention; prefer it over external TODO files.

## Tech stack

- Java 24 (records, pattern matching, `Matcher.replaceAll(Function)`).
- Lombok 1.18.38.
- Vavr 0.10.6 — used sparingly (`Option` in `CharsetDetector` and `SubtitleCleanerService`); rest of code uses Java `Optional`.
- Apache Commons Lang3 3.18.
- juniversalchardet 2.5.0.
- Spock 2.4-M6, Groovy 4.0.28.

## Locale

UI strings and logs are English. User-facing prose in this repo stays English regardless of conversation language.
