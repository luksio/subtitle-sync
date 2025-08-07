package app.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SubtitleEntry(int index, Duration start, Duration end, String text) {

    public static SubtitleEntry parse(int index, String timeLine, String text) {
        Pattern pattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})");
        Matcher m = pattern.matcher(timeLine);
        Duration start = Duration.ZERO;
        Duration end = Duration.ZERO;
        if (m.find()) start = toDuration(m);
        if (m.find()) end = toDuration(m);
        return new SubtitleEntry(index, start, end, text);
    }

    private static Duration toDuration(Matcher m) {
        return Duration.ofHours(Integer.parseInt(m.group(1)))
                .plusMinutes(Integer.parseInt(m.group(2)))
                .plusSeconds(Integer.parseInt(m.group(3)))
                .plusMillis(Integer.parseInt(m.group(4)));
    }

    public SubtitleEntry shiftBySeconds(double seconds) {
        Duration shift = Duration.ofMillis((long) (seconds * 1000));
        Duration newStart = start.plus(shift).isNegative() ? Duration.ZERO : start.plus(shift);
        Duration newEnd = end.plus(shift).isNegative() ? Duration.ZERO : end.plus(shift);
        return new SubtitleEntry(index, newStart, newEnd, text);
    }

    public SubtitleEntry convertFrameRate(BigDecimal conversionRatio) {
        BigDecimal startMillisBD = BigDecimal.valueOf(start.toMillis())
                .multiply(conversionRatio)
                .setScale(3, RoundingMode.HALF_UP);
        BigDecimal endMillisBD = BigDecimal.valueOf(end.toMillis())
                .multiply(conversionRatio)
                .setScale(3, RoundingMode.HALF_UP);

        long startMillis = Math.max(0, startMillisBD.longValue());
        long endMillis = Math.max(0, endMillisBD.longValue());

        Duration newStart = Duration.ofMillis(startMillis);
        Duration newEnd = Duration.ofMillis(endMillis);

        return new SubtitleEntry(index, newStart, newEnd, text);
    }

    public String toSrtBlock() {
        return "%d\n%s --> %s\n%s".formatted(index, formatTime(start), formatTime(end), text);
    }

    private String formatTime(Duration duration) {
        return String.format("%02d:%02d:%02d,%03d",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart(),
                duration.toMillisPart());
    }
}
