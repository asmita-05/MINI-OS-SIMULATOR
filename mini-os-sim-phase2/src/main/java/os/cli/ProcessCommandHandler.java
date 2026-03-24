package os.cli;

import os.process.Process;
import os.process.ProcessManager;
import os.utils.CommandInput;
import os.utils.Formatter;

import java.util.List;
import java.util.Optional;

/**
 * Handles all process-management commands:
 *
 *   proc add  <name> <arrival> <burst> [priority]  — create a process
 *   proc list                                       — list all processes
 *   proc rm   <pid>                                 — remove a process
 *   proc info <pid>                                 — detailed process info
 *   proc clear                                      — remove all processes
 */
public class ProcessCommandHandler implements CommandHandler {

    @Override
    public boolean handles(String command) {
        return command.equals("proc") || command.equals("process") || command.equals("ps");
    }

    @Override
    public String execute(CommandInput input, SimulationContext ctx) {
        ProcessManager pm  = ctx.getProcessManager();
        String         sub = input.getArg(0).toLowerCase();

        return switch (sub) {
            case "add", "create", "new" -> handleAdd(input, ctx);
            case "list", "ls", ""       -> handleList(pm);
            case "rm", "remove", "kill" -> handleRemove(input, pm);
            case "info"                 -> handleInfo(input, pm);
            case "clear", "reset"       -> handleClear(pm);
            default                     -> Formatter.red("Unknown sub-command '" + sub + "'. " +
                                           "Use: add | list | rm | info | clear\n");
        };
    }

    // ── sub-commands ────────────────────────────────────────────────────────

    private String handleAdd(CommandInput input, SimulationContext ctx) {
        // proc add <name> <arrival> <burst> [priority=0]
        if (input.getArgCount() < 4)
            return Formatter.red("Usage: proc add <name> <arrivalTime> <burstTime> [priority]\n");

        String name    = input.getArg(1);
        int arrival    = input.getIntArg(2, -1);
        int burst      = input.getIntArg(3, -1);
        int priority   = input.getIntArg(4, 0);

        if (arrival < 0 || burst < 1)
            return Formatter.red("Invalid arrival or burst time. Arrival >= 0, Burst >= 1.\n");

        try {
            Process p = ctx.getProcessManager().createProcess(name, arrival, burst, priority);
            ctx.getProcessManager().admit(p);

            StringBuilder sb = new StringBuilder(Formatter.green(
                    "✓ Process created: " + p + "\n"));

            // Auto-allocate memory if configured
            if (ctx.isAutoAllocMemory()) {
                int size  = ctx.getDefaultMemoryPerProc();
                int start = ctx.getMemoryManager().allocate(p.getPid(), p.getName(), size);
                if (start >= 0) {
                    p.setMemoryStart(start);
                    p.setMemorySize(size);
                    sb.append(Formatter.cyan(
                            "  → Memory allocated: " + size + " units @ address " + start + "\n"));
                } else {
                    sb.append(Formatter.yellow(
                            "  ⚠ Auto-memory allocation failed (not enough free memory).\n"));
                }
            }
            return sb.toString();
        } catch (IllegalArgumentException e) {
            return Formatter.red("Error: " + e.getMessage() + "\n");
        }
    }

    private String handleList(ProcessManager pm) {
        return Formatter.section("Processes") + Formatter.formatProcessList(pm.getAll());
    }

    private String handleRemove(CommandInput input, ProcessManager pm) {
        if (input.getArgCount() < 2)
            return Formatter.red("Usage: proc rm <pid>\n");
        int pid = input.getIntArg(1, -1);
        if (pid < 0) return Formatter.red("Invalid PID.\n");
        return pm.removeProcess(pid)
                ? Formatter.green("✓ Process P" + pid + " removed.\n")
                : Formatter.red("Process P" + pid + " not found or cannot be removed.\n");
    }

    private String handleInfo(CommandInput input, ProcessManager pm) {
        if (input.getArgCount() < 2)
            return Formatter.red("Usage: proc info <pid>\n");
        int pid = input.getIntArg(1, -1);
        Optional<Process> opt = pm.findByPid(pid);
        if (opt.isEmpty()) return Formatter.red("Process P" + pid + " not found.\n");

        Process p = opt.get();
        return String.format(
                "\n%s\n" +
                "  PID          : %d\n" +
                "  Name         : %s\n" +
                "  State        : %s\n" +
                "  Arrival Time : %d\n" +
                "  Burst Time   : %d\n" +
                "  Remaining    : %d\n" +
                "  Priority     : %d\n" +
                "  Memory       : %s\n",
                Formatter.bold("─── Process Info: P" + pid + " ──────────────────────────"),
                p.getPid(), p.getName(), p.getState(),
                p.getArrivalTime(), p.getBurstTime(), p.getRemainingTime(), p.getPriority(),
                p.getMemoryStart() >= 0
                        ? p.getMemorySize() + " units @ addr " + p.getMemoryStart()
                        : "not allocated");
    }

    private String handleClear(ProcessManager pm) {
        int count = pm.size();
        pm.clear();
        return Formatter.yellow("✓ Cleared " + count + " process(es).\n");
    }

    @Override
    public String helpText() {
        return "proc add <name> <arrival> <burst> [pri]  |  proc list  |  proc rm <pid>  |  proc info <pid>  |  proc clear";
    }
}
