package app.exception;

import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

@Builder
public class InvalidSubtitleException extends IllegalArgumentException {

    private final Integer subtitleIndex;
    private final Duration startTime;
    private final Duration endTime;
    private final String subtitleText;
    private final String baseMessage;
    private final Throwable originalCause;

    private InvalidSubtitleException(Integer subtitleIndex, Duration startTime, Duration endTime,
                                     String subtitleText, String baseMessage, Throwable originalCause) {
        super(buildDetailedMessage(baseMessage, subtitleIndex, startTime, endTime, subtitleText), originalCause);
        this.subtitleIndex = subtitleIndex;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subtitleText = subtitleText;
        this.baseMessage = baseMessage;
        this.originalCause = originalCause;
    }

    public static InvalidSubtitleException invalidIndex(int index, Duration start, Duration end, String text) {
        return InvalidSubtitleException.builder()
                .baseMessage("Subtitle index must be positive, got: " + index)
                .subtitleIndex(index)
                .startTime(start)
                .endTime(end)
                .subtitleText(text)
                .build();
    }

    public static InvalidSubtitleException nullStartTime(int index, Duration end, String text) {
        return InvalidSubtitleException.builder()
                .baseMessage("Start time cannot be null")
                .subtitleIndex(index)
                .endTime(end)
                .subtitleText(text)
                .build();
    }

    public static InvalidSubtitleException nullEndTime(int index, Duration start, String text) {
        return InvalidSubtitleException.builder()
                .baseMessage("End time cannot be null")
                .subtitleIndex(index)
                .startTime(start)
                .subtitleText(text)
                .build();
    }

    public static InvalidSubtitleException negativeStartTime(int index, Duration start, Duration end, String text) {
        return InvalidSubtitleException.builder()
                .baseMessage("Start time cannot be negative")
                .subtitleIndex(index)
                .startTime(start)
                .endTime(end)
                .subtitleText(text)
                .build();
    }

    public static InvalidSubtitleException endBeforeStart(int index, Duration start, Duration end, String text) {
        return InvalidSubtitleException.builder()
                .baseMessage("End time must be after start time")
                .subtitleIndex(index)
                .startTime(start)
                .endTime(end)
                .subtitleText(text)
                .build();
    }

    public static InvalidSubtitleException blankText(int index, Duration start, Duration end, String text) {
        return InvalidSubtitleException.builder()
                .baseMessage("Subtitle text cannot be empty or blank")
                .subtitleIndex(index)
                .startTime(start)
                .endTime(end)
                .subtitleText(text)
                .build();
    }

    public static InvalidSubtitleException emptyTimeline(int index, String text) {
        return InvalidSubtitleException.builder()
                .baseMessage("Timeline cannot be empty")
                .subtitleIndex(index)
                .subtitleText(text)
                .build();
    }

    public static InvalidSubtitleException invalidTimelineFormat(int index, String timeline, String text) {
        return InvalidSubtitleException.builder()
                .baseMessage("Invalid timeline format: " + timeline)
                .subtitleIndex(index)
                .subtitleText(text)
                .build();
    }

    public static InvalidSubtitleException missingEndTime(int index, String timeline, Duration start, String text) {
        return InvalidSubtitleException.builder()
                .baseMessage("Invalid timeline format - missing end time: " + timeline)
                .subtitleIndex(index)
                .startTime(start)
                .subtitleText(text)
                .build();
    }

    public static InvalidSubtitleException invalidTimeFormat(String timeGroup, Throwable cause) {
        return InvalidSubtitleException.builder()
                .baseMessage("Invalid time format in timeline: " + timeGroup)
                .originalCause(cause)
                .build();
    }

    public static InvalidSubtitleException undetectableCharset(String fileName) {
        return InvalidSubtitleException.builder()
                .baseMessage("Could not detect charset for file: " + fileName + ". The file may be corrupted or use an unsupported encoding.")
                .build();
    }

    private static String buildDetailedMessage(String baseMessage, Integer subtitleIndex, Duration startTime, Duration endTime, String subtitleText) {
        StringBuilder sb = new StringBuilder();

        if (subtitleIndex != null) {
            sb.append("Subtitle #").append(subtitleIndex).append(": ");
        }

        sb.append(baseMessage);

        if (startTime != null || endTime != null) {
            sb.append(" [");
            if (startTime != null) {
                sb.append("start: ").append(formatDuration(startTime));
            }
            if (endTime != null) {
                if (startTime != null) sb.append(", ");
                sb.append("end: ").append(formatDuration(endTime));
            }
            sb.append("]");
        }

        if (StringUtils.isNotBlank(subtitleText)) {
            String preview = subtitleText.length() > 50
                    ? subtitleText.substring(0, 47) + "..."
                    : subtitleText;
            sb.append(" Text: \"").append(preview.replace("\n", "\\n")).append("\"");
        }

        return sb.toString();
    }

    private static String formatDuration(Duration duration) {
        if (duration == null) return "null";

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        long millis = duration.toMillisPart();

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }
}
