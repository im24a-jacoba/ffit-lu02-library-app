package ch.bzz.command;

/**
 * Terminates the application.
 */
public class QuitCommand implements Command {

    @Override
    public String getName() {
        return "quit";
    }

    @Override
    public String getDescription() {
        return "Beendet das Programm";
    }

    @Override
    public boolean execute(String[] arguments) {
        System.out.println("Programm wird beendet.");
        return false;
    }
}
