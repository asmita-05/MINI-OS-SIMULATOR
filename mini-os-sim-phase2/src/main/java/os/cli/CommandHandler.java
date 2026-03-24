package os.cli;

import os.utils.CommandInput;

/**
 * Strategy interface for CLI command handlers.
 *
 * Each command (or group of related commands) is encapsulated in its own
 * handler class.  The shell dispatches to the appropriate handler based on
 * the primary command token returned by {@link #handles(String)}.
 */
public interface CommandHandler {

    /**
     * Returns true if this handler is responsible for the given command token.
     * The token is already lower-cased by the shell before calling this method.
     */
    boolean handles(String command);

    /**
     * Executes the command and returns a printable result string.
     *
     * @param input   fully parsed input (command + args)
     * @param context shared simulation context
     * @return output to print; empty string means no output
     */
    String execute(CommandInput input, SimulationContext context);

    /**
     * Returns a one-line usage hint shown by the "help" command.
     */
    String helpText();
}
