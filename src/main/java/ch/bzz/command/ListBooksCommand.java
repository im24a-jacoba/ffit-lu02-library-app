package ch.bzz.command;

import ch.bzz.db.BookPersistor;
import ch.bzz.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Lists the books of the database, optionally limited to the first n entries.
 */
public class ListBooksCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(ListBooksCommand.class);

    private final BookPersistor bookPersistor;

    public ListBooksCommand(BookPersistor bookPersistor) {
        this.bookPersistor = bookPersistor;
    }

    @Override
    public String getName() {
        return "listBooks";
    }

    @Override
    public String getArguments() {
        return "[limit]";
    }

    @Override
    public String getDescription() {
        return "Listet die Buecher auf, optional nur die ersten <limit>";
    }

    @Override
    public boolean execute(String[] arguments) {
        int limit;

        try {
            limit = parseLimit(arguments);
        } catch (IllegalArgumentException e) {
            // The application must keep running, the incident is only logged and reported.
            log.warn("Invalid limit for listBooks: {}", e.getMessage());
            System.out.println(e.getMessage());
            return true;
        }

        List<Book> books = bookPersistor.getAll(limit);

        if (books.isEmpty()) {
            System.out.println("Keine Buecher gefunden.");
            return true;
        }

        books.forEach(System.out::println);
        return true;
    }

    private int parseLimit(String[] arguments) {
        if (arguments.length == 0) {
            return 0;
        }

        try {
            int limit = Integer.parseInt(arguments[0]);
            if (limit < 0) {
                throw new IllegalArgumentException(
                        "Das Limit '" + arguments[0] + "' darf nicht negativ sein.");
            }
            return limit;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Das Limit '" + arguments[0] + "' ist keine gueltige Zahl.", e);
        }
    }
}
