package os.utils;

import java.util.Arrays;

/**
 * Thin wrapper around raw CLI input.
 *
 * Splits the input line into a command token and an argument array,
 * and provides typed accessors for common argument shapes.
 */
public class CommandInput {

    private final String   raw;
    private final String   command;
    private final String[] args;

    public CommandInput(String rawLine) {
        this.raw = rawLine == null ? "" : rawLine.strip();
        String[] tokens = this.raw.isEmpty() ? new String[0] : this.raw.split("\\s+");
        this.command = tokens.length > 0 ? tokens[0].toLowerCase() : "";
        this.args    = tokens.length > 1 ? Arrays.copyOfRange(tokens, 1, tokens.length)
                                         : new String[0];
    }

    public String   getRaw()             { return raw; }
    public String   getCommand()         { return command; }
    public String[] getArgs()            { return args; }
    public int      getArgCount()        { return args.length; }
    public boolean  hasArgs()            { return args.length > 0; }

    public String getArg(int index) {
        return index < args.length ? args[index] : "";
    }

    /** Returns the integer value of argument at {@code index}, or {@code defaultValue} on parse failure. */
    public int getIntArg(int index, int defaultValue) {
        try { return Integer.parseInt(getArg(index)); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    /** Joins all arguments from {@code fromIndex} onwards into a single string. */
    public String joinArgs(int fromIndex) {
        if (fromIndex >= args.length) return "";
        return String.join(" ", Arrays.copyOfRange(args, fromIndex, args.length));
    }

    public boolean isEmpty() { return command.isEmpty(); }
}
