# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

**Subtitle Sync** is a Java 24 desktop GUI application (Swing) for synchronizing and converting SRT subtitle files:
- **Time Offset Adjustment** - shift subtitle timings by seconds
- **Frame Rate Conversion** - convert between different frame rates (e.g., 23.976 ↔ 25 FPS)
- **FFprobe Integration** - automatic frame rate detection from video files

## Build & Run

- **Build tool:** Maven 3.14+ (with Makefile wrapper)
- **Commands:** Run `make help` for all targets, or use standard Maven commands
- **Main class:** `app.SubtitleSyncApp`

## Architecture

### Package Structure

```
app/
├── SubtitleSyncApp              # Main entry point (JFrame)
├── ui/                          # Swing GUI
│   ├── SubtitleSyncPanel        # Main tabbed interface
│   └── FileChooserHelper        # File selection dialogs
├── service/                     # Business logic
│   ├── SubtitleService          # SRT parsing, shifting, conversion
│   └── VideoMetadataService     # FFprobe integration
├── model/                       # Domain models
│   ├── SubtitleEntry (record)   # Immutable subtitle with Duration-based timing
│   └── FrameRate (enum)         # Supported FPS with BigDecimal precision
├── util/                        # Utilities
│   └── CharsetDetector          # Automatic encoding detection (juniversalchardet)
└── exception/
    └── InvalidSubtitleException # Validation errors
```

### Key Design Patterns

1. **Record with Validation** - `SubtitleEntry` uses compact constructor for immutable validated data
2. **Functional Transformation** - operations return new instances (no mutation)
3. **Functional Error Handling** - `CharsetDetector` returns Vavr `Option<Charset>` instead of null
4. **Enum with Behavior** - `FrameRate` encapsulates FPS values and conversion logic
5. **Utility Class Pattern** - stateless utilities use Lombok `@UtilityClass`

### Time Precision

- `java.time.Duration` for subtitle timing (nanosecond precision internally)
- `BigDecimal` for frame rate conversions (10 decimal places, HALF_UP rounding)
- SRT output format uses millisecond precision

## Technology Stack

- **Java 24** - records, pattern matching, modern features
- **Lombok 1.18.38** - `@Log`, `@UtilityClass`, `@Getter`, `@AllArgsConstructor`
- **Vavr 0.10.6** - functional programming (`Option`, `Either`, `Try`)
- **Apache Commons Lang3 3.18** - `StringUtils`
- **juniversalchardet 2.5.0** - automatic charset detection (Mozilla's UniversalChardet)
- **Spock 2.4-M6** + Groovy 4.0.28 - testing with Given-When-Then syntax
- **Swing** - GUI framework

## Development Notes

### Character Encoding

- **Automatic detection** via juniversalchardet (detects UTF-8, windows-1250, ISO-8859-1, ISO-8859-2, etc.)
- Uses buffered reading (4KB chunks) for efficiency
- Two detection modes:
  - `detectCharset()` - returns Vavr `Option<Charset>`, may return None
  - `detectCharsetWithFallback()` - always returns Charset, uses fallback when detection fails
- **Default fallback**: Windows-1250 (most common for Central European subtitles)
- Output always written in UTF-8

### SRT Format

- Format: `index\ntimeline\ntext\n\n`
- Timeline: `HH:MM:SS,mmm --> HH:MM:SS,mmm`
- Parser handles BOM (Byte Order Mark) at file start
- Parser is lenient with whitespace and blank lines
- Multi-line subtitle text is preserved

### Output File Naming

- Time-shifted: `{original}_shifted.srt`
- Frame rate converted: `{original}_{from_fps}_to_{to_fps}.srt`
  - Example: `movie_25_fps_to_23_976_fps.srt`

### FFprobe Integration

- Requires FFprobe in system PATH
- Extracts `r_frame_rate` from video stream metadata
- Matches detected FPS to closest `FrameRate` enum (within 0.5 FPS tolerance)
- Gracefully degrades if FFprobe unavailable

### Locale

- UI strings are in Polish (hardcoded, no i18n)

## Testing

- **Framework:** Spock 2.4-M6 (Groovy 4.0.28)
- **Test location:** `src/test/groovy/app/`
- **Test resources:** `src/test/resources/subtitles/` (sample SRT files in various encodings)
- **Style:** Given-When-Then syntax with `@TempDir` for file handling
