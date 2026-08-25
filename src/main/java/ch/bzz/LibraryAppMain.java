package ch.bzz;



import java.util.Scanner;



public class LibraryAppMain {

    // Enum für alle verfügbaren Befehle
    private enum Command {
        HELP("Zeigt diese Hilfe an"),
        QUIT("Beendet das Programm");

        private final String description;

        Command(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            try {
                // String-Eingabe case-insensitive zu Enum auflösen
                Command cmd = Command.valueOf(input.toUpperCase());

                switch (cmd) {
                    case QUIT -> {
                        System.out.println("Programm wird beendet.");
                        running = false;
                    }
                    case HELP -> {
                        System.out.println("Verfügbare Befehle:");
                        for (Command c : Command.values()) {
                            System.out.println("- " + c.name().toLowerCase() + ": " + c.getDescription());
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Befehl '" + input + "' nicht erkannt. Geben Sie 'help' ein für eine Übersicht.");
            }
        }

        scanner.close();
    }
}