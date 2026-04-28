package os.cli;

import os.utils.CommandInput;
import os.utils.Formatter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Batch script executor.
 *
 * Reads a plain-text script file where each non-blank, non-comment line
 * is a shell command.  Lines starting with '#' are comments.
 *
 * Script format example (my_session.ossim):
 * ─────────────────────────────────────────────────────────────────
 * # Demo script for Mini OS Simulator
 * mem init 512 first-fit
 * proc add P1 0 10 3
 * proc add P2 1  4 1
 * proc add P3 2  6 4
 * sched set rr 3
 * sched run
 * mem map
 * ─────────────────────────────────────────────────────────────────
 *
 * Usage from the shell:  run <path>
 */
public class BatchRunner {

    private final Shell           shell;
    private final SimulationContext ctx;
    private final List<CommandHandler> handlers;

    public BatchRunner(Shell shell, SimulationContext ctx, List<CommandHandler> handlers) {
        this.shell    = shell;
        this.ctx      = ctx;
        this.handlers = handlers;
    }

    /**
     * Executes the script at {@code filePath}.
     *
     * @return aggregated output from all commands
     */
    public String run(String filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append(Formatter.cyan("── Running script: " + filePath + " ──\n"));

        List<String> lines = readLines(filePath);
        if (lines == null) {
            return Formatter.red("Cannot read script file: " + filePath + "\n");
        }

        int lineNum = 0;
        for (String raw : lines) {
            lineNum++;
            String line = raw.strip();

            // Skip comments and blank lines
            if (line.isEmpty() || line.startsWith("#")) continue;

            sb.append(Formatter.yellow("  [" + lineNum + "] ")).append(line).append("\n");

            CommandInput input = new CommandInput(line);
            if (input.isEmpty()) continue;

            if (input.getCommand().equals("exit") || input.getCommand().equals("quit")) {
                sb.append(Formatter.yellow("  Script ended via exit command.\n"));
                break;
            }

            String result = dispatch(input);
            if (result != null && !result.isEmpty()) sb.append(result);
        }

        sb.append(Formatter.cyan("── Script complete ──\n"));
        return sb.toString();
    }

    private String dispatch(CommandInput input) {
        for (CommandHandler h : handlers) {
            if (h.handles(input.getCommand())) {
                try { return h.execute(input, ctx); }
                catch (Exception e) {
                    return Formatter.red("[ERROR] " + e.getMessage() + "\n");
                }
            }
        }
        return Formatter.red("Unknown command: '" + input.getCommand() + "'\n");
    }

    private List<String> readLines(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) lines.add(line);
            return lines;
        } catch (IOException e) {
            return null;
        }
    }
}
