package ch.bzz;

import ch.bzz.command.Command;
import ch.bzz.command.CommandRegistry;
import ch.bzz.command.CreateUserCommand;
import ch.bzz.command.HelpCommand;
import ch.bzz.command.ImportBooksCommand;
import ch.bzz.command.ListBooksCommand;
import ch.bzz.command.QuitCommand;
import ch.bzz.db.BookPersistor;
import ch.bzz.db.UserPersistor;
import ch.bzz.io.BookTsvReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console user interface of the library application. It only reads the input and
 * delegates to the matching {@link Command} - the logic itself lives in the
 * commands and the classes they use (SRP).
 */
public class LibraryAppMain {

    private static final Logger log = LoggerFactory.getLogger(LibraryAppMain.class);

    private static final String PROMPT = "> ";

    public static void main(String[] args) {
        log.info("Library application started");

        CommandRegistry registry = createRegistry();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running && scanner.hasNextLine()) {
            System.out.print(PROMPT);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            running = handle(registry, input);
        }

        scanner.close();
        log.info("Library application stopped");
    }

    /**
     * Registers all available commands. A new command only has to be added here.
     */
    private static CommandRegistry createRegistry() {
        BookPersistor bookPersistor = new BookPersistor();
        UserPersistor userPersistor = new UserPersistor();

        CommandRegistry registry = new CommandRegistry();
        registry.register(new HelpCommand(registry));
        registry.register(new ListBooksCommand(bookPersistor));
        registry.register(new ImportBooksCommand(new BookTsvReader(), bookPersistor));
        registry.register(new CreateUserCommand(userPersistor));
        registry.register(new QuitCommand());

        return registry;
    }

    /**
     * @return false if the application should terminate
     */
    private static boolean handle(CommandRegistry registry, String input) {
        String[] parts = input.split("\\s+");
        String name = parts[0];
        String[] arguments = Arrays.copyOfRange(parts, 1, parts.length);

        Optional<Command> command = registry.find(name);

        if (command.isEmpty()) {
            log.debug("Unknown command '{}'", name);
            System.out.println("Befehl '" + input + "' nicht erkannt. Mit 'help' "
                    + "werden alle Befehle aufgelistet.");
            return true;
        }

        try {
            return command.get().execute(arguments);
        } catch (RuntimeException e) {
            // A single failing command must not crash the whole application.
            log.error("Unexpected error while executing the command '{}'", input, e);
            System.out.println("Der Befehl '" + name + "' konnte nicht ausgefuehrt werden.");
            return true;
        }
    }
}
