package ch.bzz;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class LibraryAppMainTest {

    @Test
    void testQuitEndsProgramWithoutError() {
        // Arrange
        prepareStreams("quit\n");

        // Act & Assert
        assertDoesNotThrow(() -> LibraryAppMain.main(new String[]{}),
                "Program should terminate cleanly without throwing an exception");
    }

    @Test
    void testInvalidCommandContainsInput() {
        // Arrange
        var outStream = prepareStreams("foobar\nquit\n");

        // Act
        LibraryAppMain.main(new String[]{});

        // Assert
        var consoleOutput = outStream.toString();
        assertTrue(consoleOutput.contains("foobar"),
                "Output should contain the invalid command itself");
    }

    @Test
    void testHelpCommandContainsHelpAndQuit() {
        // Arrange
        var outStream = prepareStreams("help\nquit\n");

        // Act
        LibraryAppMain.main(new String[]{});

        // Assert
        var consoleOutput = outStream.toString();
        assertTrue(consoleOutput.contains("help"), "Output should contain 'help'");
        assertTrue(consoleOutput.contains("quit"), "Output should contain 'quit'");
    }

    private ByteArrayOutputStream prepareStreams(String input) {
        var in = new ByteArrayInputStream(input.getBytes());
        var out = new ByteArrayOutputStream();

        System.setIn(in);
        System.setOut(new PrintStream(out));

        return out;
    }
}
