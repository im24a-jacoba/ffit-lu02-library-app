package ch.bzz;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        assertTrue(consoleOutput.contains("listBooks"), "Output should contain 'listBooks'");
        assertTrue(consoleOutput.contains("importBooks"), "Output should contain 'importBooks'");
    }

    @Test
    void testListBooksPrintsExampleBooks() {
        // Arrange
        var out = prepareStreams("listBooks\nquit\n");

        // Act
        LibraryAppMain.main(new String[]{});

        // Assert
        var output = out.toString();
        assertTrue(output.contains("Effective Java"), "Output should contain the title of the first book");
        assertTrue(output.contains("Head First Java"), "Output should contain the title of the second book");
    }

    @Test
    void testImportBooksImportsFromCsv() throws URISyntaxException {
        // Arrange
        var resourceUrl = getClass().getClassLoader().getResource("test_books_import.tsv");
        assertNotNull(resourceUrl, "Test resource file should exist");
        Path filePath = Paths.get(resourceUrl.toURI());
        var out = prepareStreams("importBooks " + filePath + "\nlistBooks\nquit\n");

        // Act
        LibraryAppMain.main(new String[]{});

        // Assert
        String output = out.toString();

        assertTrue(output.contains("Domain-Driven Design"), "Output should contain imported book title");
        assertTrue(output.contains("Refactoring"), "Output should contain imported book title");
        assertTrue(output.contains("Clean Architecture"), "Output should contain imported book title");
    }


    private ByteArrayOutputStream prepareStreams(String input) {
        var in = new ByteArrayInputStream(input.getBytes());
        var out = new ByteArrayOutputStream();

        System.setIn(in);
        System.setOut(new PrintStream(out));

        return out;
    }
}
