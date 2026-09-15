package ch.bzz;

import ch.bzz.db.BookPersistor;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API of the library application. It exposes the same logic as the console
 * commands, but over HTTP: GET http://localhost:7070/books?limit=10
 */
public class JavalinMain {

    private static final Logger log = LoggerFactory.getLogger(JavalinMain.class);

    private static final int PORT = 7070;
    private static final String LIMIT_PARAM = "limit";

    private static final BookPersistor BOOK_PERSISTOR = new BookPersistor();

    public static void main(String[] args) {
        Javalin app = Javalin.create();

        app.get("/books", JavalinMain::getBooks);

        app.start(PORT);
        log.info("REST API started on port {}", PORT);
    }

    private static void getBooks(Context ctx) {
        int limit = parseLimit(ctx.queryParam(LIMIT_PARAM));
        ctx.json(BOOK_PERSISTOR.getAll(limit));
    }

    /**
     * @return the limit, or 0 (no limit) if the query parameter is missing or not a number
     */
    private static int parseLimit(String value) {
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
