package os.cli;

import os.process.Process;
import os.scheduler.ScheduleResult;
import os.scheduler.SchedulerFactory;
import os.utils.CommandInput;
import os.utils.Formatter;

import java.util.List;
import java.util.Map;

/**
 * Handles scheduling commands:
 *
 *   sched set <algo> [quantum]   — select a scheduling algorithm
 *   sched run                    — run simulation on current process list
 *   sched info                   — show active scheduler
 *   sched list                   — list available algorithms
 */
public class SchedulerCommandHandler implements CommandHandler {

    @Override
    public boolean handles(String command) {
        return command.equals("sched") || command.equals("scheduler");
    }

    @Override
    public String execute(CommandInput input, SimulationContext ctx) {
        String sub = input.getArg(0).toLowerCase();
        return switch (sub) {
            case "set"               -> handleSet(input, ctx);
            case "run", "simulate"   -> handleRun(ctx);
            case "info", "current"   -> handleInfo(ctx);
            case "list", "ls", "help" -> handleList();
            default                  -> Formatter.red(
                    "Unknown sub-command '" + sub + "'. Use: set | run | info | list\n");
        };
    }

    // ── sub-commands ────────────────────────────────────────────────────────

    private String handleSet(CommandInput input, SimulationContext ctx) {
        // sched set <algo> [quantum]
        if (input.getArgCount() < 2)
            return Formatter.red("Usage: sched set <algo> [quantum]\n       e.g. sched set rr 4\n");

        String key     = input.getArg(1).toLowerCase();
        int    quantum = input.getIntArg(2, ctx.getDefaultQuantum());

        try {
            ctx.setScheduler(key, quantum);
            return Formatter.green("✓ Scheduler set to: " + ctx.getActiveScheduler().getName() + "\n");
        } catch (IllegalArgumentException e) {
            return Formatter.red("Error: " + e.getMessage() + "\n");
        }
    }

    private String handleRun(SimulationContext ctx) {
        List<Process> procs = ctx.getProcessManager().getAll();
        if (procs.isEmpty())
            return Formatter.yellow("No processes defined. Use 'proc add' to create some.\n");

        StringBuilder sb = new StringBuilder();
        sb.append(Formatter.section("Scheduling Simulation: " + ctx.getActiveScheduler().getName()));
        sb.append(Formatter.cyan("  Running " + procs.size() + " process(es)...\n"));

        try {
            ScheduleResult result = ctx.getActiveScheduler().simulate(procs);

            sb.append(Formatter.section("Gantt Chart"));
            sb.append(Formatter.formatGantt(result));

            sb.append(Formatter.section("Per-Process Metrics"));
            sb.append(Formatter.formatProcessTable(result));

            sb.append(Formatter.formatMetrics(result));

        } catch (Exception e) {
            sb.append(Formatter.red("Simulation failed: " + e.getMessage() + "\n"));
        }
        return sb.toString();
    }

    private String handleInfo(SimulationContext ctx) {
        return Formatter.cyan("Active scheduler: ") +
               Formatter.bold(ctx.getActiveScheduler().getName()) + "\n";
    }

    private String handleList() {
        StringBuilder sb = new StringBuilder(Formatter.section("Available Scheduling Algorithms"));
        Map<String, String> descs = SchedulerFactory.getDescriptions();
        for (Map.Entry<String, String> entry : descs.entrySet())
            sb.append(String.format("  %-14s %s\n",
                    Formatter.cyan(entry.getKey()), entry.getValue()));
        sb.append("\n").append(Formatter.yellow("Usage: sched set <key> [quantum]\n"));
        return sb.toString();
    }

    @Override
    public String helpText() {
        return "sched set <algo> [quantum]  |  sched run  |  sched info  |  sched list";
    }
}
