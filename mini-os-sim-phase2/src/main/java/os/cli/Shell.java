package os.cli;

import os.utils.CommandInput;
import os.utils.Formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive CLI shell — Phase 2 build.
 *
 * Implemented : proc | sched | mem | fs commands | help | status
 * Phase 3     : compare | batch scripting | paging | advanced algorithms
 */
public class Shell {

    private final SimulationContext    context;
    private final List<CommandHandler> handlers;
    private final CommandHistory       history;

    public Shell() {
        this.context  = new SimulationContext();
        this.handlers = new ArrayList<>();
        this.history  = new CommandHistory(100);
        registerHandlers();
    }

    private void registerHandlers() {
        handlers.add(new ProcessCommandHandler());
        handlers.add(new SchedulerCommandHandler());
        handlers.add(new MemoryCommandHandler());
        handlers.add(new FileSystemCommandHandler());
        handlers.add(new HelpCommandHandler(handlers));
    }

    public SimulationContext    getContext()  { return context; }
    public List<CommandHandler> getHandlers() { return handlers; }

    public void run() {
        printBanner();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printPrompt();
            if (!scanner.hasNextLine()) break;
            String line    = scanner.nextLine();
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) history.add(trimmed);

            CommandInput input = new CommandInput(trimmed);
            if (input.isEmpty()) continue;

            switch (input.getCommand()) {
                case "exit", "quit" -> {
                    System.out.println(Formatter.cyan("\nSession ended. See you in Phase 3!\n"));
                    return;
                }
                case "cls", "clear" -> {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    continue;
                }
                case "history" -> {
                    System.out.print(formatHistory());
                    continue;
                }
                case "run", "compare", "page" -> {
                    System.out.print(Formatter.yellow(
                        "[Phase 3] '" + input.getCommand() + "' is not yet implemented. " +
                        "Coming in the next phase!\n"));
                    continue;
                }
            }

            String output = dispatch(input);
            if (output != null && !output.isEmpty()) System.out.print(output);
        }
        scanner.close();
    }

    String dispatch(CommandInput input) {
        for (CommandHandler h : handlers) {
            if (h.handles(input.getCommand())) {
                try { return h.execute(input, context); }
                catch (Exception e) {
                    return Formatter.red("[ERROR] " + e.getClass().getSimpleName()
                            + ": " + e.getMessage() + "\n");
                }
            }
        }
        return Formatter.red("Unknown command: '" + input.getCommand()
                + "'. Type 'help' for available commands.\n");
    }

    private void printPrompt() {
        String cwd   = context.getFileSystem().pwd();
        int    procs = context.getProcessManager().size();
        System.out.print(
                Formatter.GREEN  + "os-sim"  + Formatter.RESET +
                Formatter.YELLOW + ":"       + Formatter.RESET +
                Formatter.BLUE   + cwd       + Formatter.RESET +
                Formatter.CYAN   + " [" + procs + "p]" + Formatter.RESET +
                "$ ");
        System.out.flush();
    }

    private void printBanner() {
        System.out.println(Formatter.banner());
        System.out.println(Formatter.yellow("  Phase 2 Build  —  Core Features Implemented\n"));
        System.out.println(Formatter.cyan(  "  Commands : proc | sched | mem | fs | help | status"));
        System.out.println(Formatter.cyan(  "  Schedulers: fcfs | sjf | rr | priority"));
        System.out.println(Formatter.yellow("  Phase 3  : compare | paging | srtf | mlq | batch scripts\n"));
    }

    private String formatHistory() {
        List<String> entries = history.getAll();
        if (entries.isEmpty()) return Formatter.yellow("  (no history)\n");
        StringBuilder sb = new StringBuilder(Formatter.section("Command History"));
        for (int i = 0; i < entries.size(); i++)
            sb.append(String.format("  %3d  %s\n", i + 1, entries.get(i)));
        return sb.toString();
    }
}
