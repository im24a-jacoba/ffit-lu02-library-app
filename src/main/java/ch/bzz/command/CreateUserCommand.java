package ch.bzz.command;

import ch.bzz.db.UserPersistor;
import ch.bzz.model.User;
import ch.bzz.security.PasswordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Creates a new user. The password is given in plaintext but only the generated
 * salt and the resulting hash are stored in the database.
 */
public class CreateUserCommand implements Command {

    private static final Logger log = LoggerFactory.getLogger(CreateUserCommand.class);

    private static final int EXPECTED_ARGUMENT_COUNT = 5;

    private final UserPersistor userPersistor;

    public CreateUserCommand(UserPersistor userPersistor) {
        this.userPersistor = userPersistor;
    }

    @Override
    public String getName() {
        return "createUser";
    }

    @Override
    public String getArguments() {
        return "<firstname> <lastname> <YYYY-MM-DD> <email> <password>";
    }

    @Override
    public String getDescription() {
        return "Legt einen neuen Benutzer an";
    }

    @Override
    public boolean execute(String[] arguments) {
        if (arguments.length != EXPECTED_ARGUMENT_COUNT) {
            System.out.println("Erwartet werden " + EXPECTED_ARGUMENT_COUNT
                    + " Argumente: createUser " + getArguments());
            return true;
        }

        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(arguments[2]);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date of birth '{}' for createUser", arguments[2]);
            System.out.println("Das Geburtsdatum '" + arguments[2]
                    + "' ist ungueltig, erwartet wird YYYY-MM-DD.");
            return true;
        }

        User user = new User(null, arguments[0], arguments[1], dateOfBirth, arguments[3]);

        try {
            applyPassword(user, arguments[4]);
        } catch (NoSuchAlgorithmException e) {
            log.error("Hash algorithm is not available, the user {} was not created", user.getEmail(), e);
            System.out.println("Der Benutzer konnte nicht angelegt werden.");
            return true;
        }

        if (userPersistor.save(user)) {
            System.out.println("Benutzer '" + user.getEmail() + "' wurde angelegt.");
        } else {
            System.out.println("Der Benutzer '" + user.getEmail() + "' konnte nicht gespeichert werden.");
        }

        return true;
    }

    private void applyPassword(User user, String password) throws NoSuchAlgorithmException {
        byte[] salt = PasswordHandler.generateSalt();
        byte[] hash = PasswordHandler.hashPassword(password, salt);

        user.setPasswordSalt(PasswordHandler.encode(salt));
        user.setPasswordHash(PasswordHandler.encode(hash));
    }
}
