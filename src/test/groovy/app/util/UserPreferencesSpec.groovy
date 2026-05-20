package app.util

import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class UserPreferencesSpec extends Specification {

    @TempDir
    Path tempDir

    def setup() {
        UserPreferences.clear()
    }

    def cleanup() {
        UserPreferences.clear()
    }

    def 'should return empty when no last directory has been stored'() {
        expect:
            UserPreferences.getLastDirectory().isEmpty()
    }

    def 'should round-trip a stored directory'() {
        given:
            def dir = Files.createDirectory(tempDir.resolve('movies')).toFile()

        when:
            UserPreferences.setLastDirectory(dir)

        then:
            UserPreferences.getLastDirectory().get() == dir
    }

    def 'should return empty when the stored path no longer exists (e.g. external drive disconnected)'() {
        given: 'a directory is stored, then removed from the filesystem'
            def dir = Files.createDirectory(tempDir.resolve('vanished')).toFile()
            UserPreferences.setLastDirectory(dir)
            dir.delete()

        expect:
            UserPreferences.getLastDirectory().isEmpty()
    }

    def 'should return empty when the stored path points to a regular file rather than a directory'() {
        given:
            def file = Files.createFile(tempDir.resolve('not-a-dir')).toFile()
            UserPreferences.setLastDirectory(file)

        expect: 'setLastDirectory rejected the non-directory, so nothing was stored'
            UserPreferences.getLastDirectory().isEmpty()
    }

    def 'should ignore null when setting last directory'() {
        when:
            UserPreferences.setLastDirectory(null)

        then:
            noExceptionThrown()
            UserPreferences.getLastDirectory().isEmpty()
    }
}
