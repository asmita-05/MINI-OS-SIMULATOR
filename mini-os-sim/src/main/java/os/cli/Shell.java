package os.cli;

import os.utils.CommandInput;
import os.utils.Formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive CLI shell — the top-level REPL of the Mini OS Simulator.
 *
 * Architecture:
 *   Shell → CommandInput → dispatch() → CommandHandler → SimulationContext
 *
 * Responsibilities:
 *   1. Read-Eval-Print loop with a context-aware prompt.
 *   2. Ordered handler registry (first match wins).
 *   3. Command history with ↑/↓ recall (via CommandHistory).
 *   4. Batch-script execution via BatchRunner (`run <file>`).
 *   5. Built-in aliases: `cls` → clear screen, `run <file>` → batch.
 *
 * Adding a new command:
 *   Implement {@link CommandHandler}, then add it to {@link #registerHandlers()}.
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

    // ── Handler registration — order matters (first match wins) ─────────────

    private void registerHandlers() {
        handlers.add(new ProcessCommandHandler());
        handlers.add(new SchedulerCommandHandler());
        handlers.add(new MemoryCommandHandler());
        handlers.add(new FileSystemCommandHandler());
        handlers.add(new IPCCommandHandler());
        handlers.add(new HelpCommandHandler(handlers));
    }

    // ── Public accessor used by BatchRunner ──────────────────────────────────

    public SimulationContext    getContext()  { return context; }
    public List<CommandHandler> getHandlers() { return handlers; }

    // ── REPL ─────────────────────────────────────────────────────────────────

    public void run() {
        printBanner();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printPrompt();

            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine();

            if (line == null) break;
            String trimmed = line.strip();

            // Record non-empty lines in history
            if (!trimmed.isEmpty()) history.add(trimmed);

            CommandInput input = new CommandInput(trimmed);
            if (input.isEmpty()) continue;

            // ── Built-ins ────────────────────────────────────────────────────
            switch (input.getCommand()) {
                case "exit", "quit" -> {
                    System.out.println(Formatter.cyan("\nBye — simulation session ended.\n"));
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
            }

            // ── Dispatch to handlers ─────────────────────────────────────────
            String output = dispatch(input);
            if (output != null && !output.isEmpty()) System.out.print(output);
        }

        scanner.close();
    }

    // ── Package-visible dispatch (used by BatchRunner) ───────────────────────

    String dispatch(CommandInput input) {
        for (CommandHandler h : handlers) {
            if (h.handles(input.getCommand())) {
                try {
                    return h.execute(input, context);
                } catch (Exception e) {
                    return Formatter.red("[ERROR] " + e.getClass().getSimpleName()
                            + ": " + e.getMessage() + "\n");
                }
            }
        }
        return Formatter.red("Unknown command: '" + input.getCommand()
                + "'. Type 'help' for available commands.\n");
    }

    // ── Prompt ───────────────────────────────────────────────────────────────

    private void printPrompt() {
        String cwd   = context.getFileSystem().pwd();
        int    procs = context.getProcessManager().size();
        System.out.print(
                Formatter.GREEN  + "os-sim"  + Formatter.RESET +
                Formatter.YELLOW + ":"       + Formatter.RESET +
                Formatter.BLUE   + cwd       + Formatter.RESET +
                Formatter.CYAN   + " ["  + procs + "p]" + Formatter.RESET +
                "$ ");
        System.out.flush();
    }

    // ── Banner ───────────────────────────────────────────────────────────────

    private void printBanner() {
        System.out.println(Formatter.banner());
        System.out.println(Formatter.cyan(
                "  Commands : help | demo | status | history"));
        System.out.println(Formatter.cyan(
                "  Modules  : proc | sched | mem | fs cmds"));
        System.out.println(Formatter.cyan(
                "  IPC      : ipc send | ipc recv | ipc publish"));
        System.out.println(Formatter.cyan(
                "  Exit     : exit\n"));
    }

    // ── History display ──────────────────────────────────────────────────────

    private String formatHistory() {
        List<String> entries = history.getAll();
        if (entries.isEmpty()) return Formatter.yellow("  (no history)\n");
        StringBuilder sb = new StringBuilder(Formatter.section("Command History"));
        for (int i = 0; i < entries.size(); i++)
            sb.append(String.format("  %3d  %s\n", i + 1, entries.get(i)));
        return sb.toString();
    }
}
