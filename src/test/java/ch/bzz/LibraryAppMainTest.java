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
    void testListBooksWithLimitOnePrintsOnlyOneBook() {
        // Arrange
        var out = prepareStreams("listBooks 1\nquit\n");

        // Act
        LibraryAppMain.main(new String[]{});

        // Assert
        var output = out.toString();
        assertTrue(output.contains("Effective Java"), "Output should contain the title of the first book");
        assertFalse(output.contains("Head First Java"), "Output should not contain the title of the second book");
    }

    @Test
    void testListBooksWithInvalidNumberDoesNotThrow() {
        // Arrange
        var out = prepareStreams("listBooks SEVEN\nquit\n");

        // Act + Assert
        assertDoesNotThrow(() -> LibraryAppMain.main(new String[]{}),
                "Invalid numeric input should not throw an exception");

        var output = out.toString();
        assertFalse(output.isEmpty(), "Output should indicate that the argument could not be parsed");
    }


    @Test
    void testImportBooksImportsFromCsv() throws URISyntaxException {
        // Arrange
        var resourceUrl = getClass().getClassLoader().getResource("test_books_import.tsv");
        assertNotNull(resourceUrl, "Test resource file should exist");
        Path filePath = Paths.get(resourceUrl.toURI());
        var out = prepareStreams("importBooks " + filePath + "\nlistBooks 4\nquit\n");

        // Act
        LibraryAppMain.main(new String[]{});

        // Assert
        String output = out.toString();

        assertTrue(output.contains("Domain-Driven Design"), "Output should contain imported book title with id=3");
        assertTrue(output.contains("Refactoring"), "Output should contain imported book title with id=4");
        assertFalse(output.contains("Clean Architecture"), "Output should not contain imported book title with id=4");
    }


    @Test
    void testImportBooksWithInvalidFileDoesNotThrow() throws URISyntaxException {
        // Arrange
        String filePath = "NONEXISTING.tsv";
        var resourceUrl = getClass().getClassLoader().getResource(filePath);
        assertNull(resourceUrl, "Test resource file should not exist");
        var out = prepareStreams("importBooks " + filePath + "\nquit\n");

        // Act + Assert
        assertDoesNotThrow(() -> LibraryAppMain.main(new String[]{}),
                "Invalid file path should not throw an exception");

        var output = out.toString();
        assertFalse(output.isEmpty(), "Output should indicate that the file could not be found");
    }


    private ByteArrayOutputStream prepareStreams(String input) {
        var in = new ByteArrayInputStream(input.getBytes());
        var out = new ByteArrayOutputStream();

        System.setIn(in);
        System.setOut(new PrintStream(out));

        return out;
    }
}
