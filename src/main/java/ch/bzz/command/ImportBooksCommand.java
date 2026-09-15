package ch.bzz.command;

import ch.bzz.db.BookPersistor;
import ch.bzz.io.BookTsvReader;
import ch.bzz.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Imports a TSV file into the database. Entries with an already existing id are
 * overwritten, so corrections can be imported with the same command.
 */
public class ImportBooksCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(ImportBooksCommand.class);

    private final BookTsvReader bookTsvReader;
    private final BookPersistor bookPersistor;

    public ImportBooksCommand(BookTsvReader bookTsvReader, BookPersistor bookPersistor) {
        this.bookTsvReader = bookTsvReader;
        this.bookPersistor = bookPersistor;
    }

    @Override
    public String getName() {
        return "importBooks";
    }

    @Override
    public String getArguments() {
        return "<FILE_PATH>";
    }

    @Override
    public String getDescription() {
        return "Importiert die Buecher der angegebenen TSV-Datei in die Datenbank";
    }

    @Override
    public boolean execute(String[] arguments) {
        if (arguments.length == 0) {
            System.out.println("Bitte einen Dateipfad angeben: importBooks <FILE_PATH>");
            return true;
        }

        // The path may contain spaces, therefore all arguments are joined again.
        String filePath = String.join(" ", arguments);

        try {
            List<Book> books = bookTsvReader.read(filePath);
            bookPersistor.saveAll(books);
            System.out.println(books.size() + " Buecher aus '" + filePath + "' importiert.");
        } catch (IOException e) {
            // The application must keep running even if the file does not exist.
            log.error("Could not read the import file {}", filePath, e);
            System.out.println("Die Datei '" + filePath + "' konnte nicht gelesen werden.");
        }

        return true;
    }
}
