package ch.bzz;

import ch.bzz.api.AuthController;
import ch.bzz.api.BookController;
import ch.bzz.db.BookPersistor;
import ch.bzz.db.UserPersistor;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API of the library application. It only wires the routes to the controllers,
 * the logic itself lives in the controllers and the classes they use (SRP).
 *
 * <pre>
 * GET  /books?limit=10
 * POST /auth/login
 * PUT  /auth/change-password
 * </pre>
 */
public class JavalinMain {

    private static final Logger log = LoggerFactory.getLogger(JavalinMain.class);

    private static final int PORT = 7070;

    public static void main(String[] args) {
        BookController bookController = new BookController(new BookPersistor());
        AuthController authController = new AuthController(new UserPersistor());

        Javalin app = Javalin.create();

        app.get("/books", bookController::getBooks);
        app.post("/auth/login", authController::login);
        app.put("/auth/change-password", authController::changePassword);

        app.start(PORT);
        log.info("REST API started on port {}", PORT);
    }
}
