package ch.bzz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.bzz.model.Book;
import ch.bzz.model.User;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

class LibraryAppMainTest {

    private static EntityManagerFactory emf;

    private static TestDatabase database;
    private final InputStream originalIn = System.in;
    private final PrintStream originalOut = System.out;

    @BeforeAll
    static void startDatabase() throws Exception {
        database = new TestDatabase();
        emf = ch.bzz.db.PersistenceManager.getEntityManagerFactory();
    }

    @org.junit.jupiter.api.BeforeEach
    void resetData() throws Exception {
        database.reset();
    }

    @org.junit.jupiter.api.AfterEach
    void restoreStreams() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    @AfterAll
    static void tearDown() throws Exception {
        database.close();
    }
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
        assertTrue(output.contains("Effective Java"), "Output should contain the title of the first book: " + output);
        assertTrue(output.contains("Head First Java"), "Output should contain the title of the second book: " + output);
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
    void testImportBooksWithInvalidFileDoesNotThrow() {
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

    @Test
    void testCreateUserCommand() {
        // Arrange
        try (var em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("DELETE FROM User u WHERE u.email = 'max.mustermann@example.com'").executeUpdate();
            em.getTransaction().commit();
        }

        String input = "createUser Max Mustermann 1990-05-21 max.mustermann@example.com geheim123\nquit\n";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);

        // Act
        LibraryAppMain.main(new String[]{});

        // Assert: Prüfen, ob User in DB existiert
        try (var em = emf.createEntityManager()) {
            User user = em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", "max.mustermann@example.com")
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            assertNotNull(user, "User should exist in database");
            assertEquals("Max", user.getFirstname());
            assertEquals("Mustermann", user.getLastname());
            assertEquals("1990-05-21", user.getDateOfBirth().toString());
            assertNotNull(user.getPasswordHash(), "PasswordHash should be set");
            assertNotNull(user.getPasswordSalt(), "PasswordSalt should be set");
        }
    }

    private ByteArrayOutputStream prepareStreams(String input) {
        var in = new ByteArrayInputStream(input.getBytes());
        var out = new ByteArrayOutputStream();

        System.setIn(in);
        System.setOut(new PrintStream(out));

        return out;
    }

    @Test
    void repeatedImportUpdatesBooksWithoutDuplicates() throws Exception {
        var file = Paths.get(getClass().getClassLoader().getResource("test_books_import.tsv").toURI());
        prepareStreams("importBooks " + file + "\nimportBooks " + file + "\nquit\n");
        LibraryAppMain.main(new String[]{});
        var books = new ch.bzz.db.BookPersistor();
        assertEquals(5, books.getAll().size());
        assertEquals("Domain-Driven Design", books.findById(3).getTitle());
    }
}
