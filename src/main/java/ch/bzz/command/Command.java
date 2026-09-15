package ch.bzz.command;

/**
 * A command that can be entered on the console. New commands only have to be
 * registered in the {@link CommandRegistry} - the help output is derived from
 * the registry, so it never has to be extended separately (DRY, OCP).
 */
public interface Command {

    /**
     * @return the name that has to be typed on the console, e.g. "listBooks"
     */
    String getName();

    /**
     * @return the arguments of the command, e.g. "[limit]" - empty if there are none
     */
    default String getArguments() {
        return "";
    }

    /**
     * @return a short description for the help output
     */
    String getDescription();

    /**
     * Executes the command.
     *
     * @param arguments the arguments entered after the command name
     * @return false if the application should terminate, true otherwise
     */
    boolean execute(String[] arguments);
}
