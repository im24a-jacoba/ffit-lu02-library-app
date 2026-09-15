package ch.bzz.command;

/**
 * Lists all registered commands. The list is built out of the registry, therefore
 * a new command automatically shows up here.
 */
public class HelpCommand implements Command {

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Listet alle verfuegbaren Befehle auf";
    }

    @Override
    public boolean execute(String[] arguments) {
        System.out.println("Verfuegbare Befehle:");

        for (Command command : registry.getCommands()) {
            String usage = command.getName();
            if (!command.getArguments().isEmpty()) {
                usage += " " + command.getArguments();
            }
            System.out.printf("  %-45s %s%n", usage, command.getDescription());
        }

        return true;
    }
}
