package ch.bzz.command;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Knows all available commands. Adding a new command means registering it here -
 * nothing else in the application has to be touched.
 */
public class CommandRegistry {

    private final Map<String, Command> commands = new LinkedHashMap<>();

    public void register(Command command) {
        commands.put(command.getName(), command);
    }

    /**
     * @param name the command name as it was entered on the console
     * @return the matching command, or empty if the name is unknown
     */
    public Optional<Command> find(String name) {
        return Optional.ofNullable(commands.get(name));
    }

    /**
     * @return all registered commands in their registration order
     */
    public Collection<Command> getCommands() {
        return commands.values();
    }
}
