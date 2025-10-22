package app.util;

import io.vavr.control.Option;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

@Log
@UtilityClass
public class CharsetDetector {

    private static final int BUFFER_SIZE = 4096;

    public static Option<Charset> detectCharset(Path filePath) {
        UniversalDetector detector = new UniversalDetector();

        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(filePath))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) > 0 && !detector.isDone()) {
                detector.handleData(buffer, 0, bytesRead);
            }

            detector.dataEnd();
        } catch (IOException e) {
            log.severe(() -> "Error reading file for encoding detection: %s - %s".formatted(filePath, e.getMessage()));
            return Option.none();
        }

        String detectedCharsetName = detector.getDetectedCharset();

        if (detectedCharsetName == null) {
            log.warning(() -> "Failed to detect file encoding: " + filePath);
            return Option.none();
        }

        try {
            Charset charset = Charset.forName(detectedCharsetName);
            return Option.of(charset);
        } catch (Exception e) {
            log.severe(() -> "Detected encoding '%s' is not supported by JVM for file: %s".formatted(detectedCharsetName, filePath));
            return Option.none();
        }
    }

    public static Charset detectCharsetWithFallback(Path filePath, Charset fallbackCharset) {
        return detectCharset(filePath)
                .onEmpty(() -> log.info(() -> "Using fallback charset '%s' for file: %s".formatted(fallbackCharset.name(), filePath)))
                .getOrElse(fallbackCharset);
    }
}
