package ch.bzz.api;

import ch.bzz.db.BookPersistor;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP endpoints for books - and nothing else (SRP).
 */
public class BookController {

    private static final Logger log = LoggerFactory.getLogger(BookController.class);

    private static final String LIMIT_PARAM = "limit";

    private final BookPersistor bookPersistor;

    public BookController(BookPersistor bookPersistor) {
        this.bookPersistor = bookPersistor;
    }

    /**
     * GET /books?limit=10
     */
    public void getBooks(Context ctx) {
        int limit = parseLimit(ctx.queryParam(LIMIT_PARAM));
        ctx.json(bookPersistor.getAll(limit));
    }

    /**
     * @return the limit, or 0 (no limit) if the query parameter is missing or not a number
     */
    private int parseLimit(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            // The API must keep running, the incident is only logged.
            log.warn("Query parameter '{}' is not a valid number: {}", LIMIT_PARAM, value);
            return 0;
        }
    }
}
