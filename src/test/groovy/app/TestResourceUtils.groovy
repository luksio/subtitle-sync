package app

import java.nio.file.Files
import java.nio.file.Path

class TestResourceUtils {

    static File copySubtitleToTemp(String resourceFileName, Path tempDir) {
        def resourcePath = "/subtitles/${resourceFileName}"
        def resourceStream = TestResourceUtils.class.getResourceAsStream(resourcePath)
        if (resourceStream == null) {
            throw new IllegalArgumentException("Resource not found: ${resourcePath}")
        }
        def targetFile = tempDir.resolve("test_${resourceFileName}").toFile()
        Files.copy(resourceStream, targetFile.toPath())
        return targetFile
    }
}
