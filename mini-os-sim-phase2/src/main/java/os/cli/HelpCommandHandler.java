package os.cli;

import os.utils.CommandInput;
import os.utils.Formatter;
import java.util.List;

/**
 * Handles meta-commands — Phase 2 build.
 */
public class HelpCommandHandler implements CommandHandler {

    private final List<CommandHandler> allHandlers;

    public HelpCommandHandler(List<CommandHandler> allHandlers) {
        this.allHandlers = allHandlers;
    }

    @Override
    public boolean handles(String command) {
        return command.equals("help") || command.equals("?")
            || command.equals("status") || command.equals("demo")
            || command.equals("version") || command.equals("info");
    }

    @Override
    public String execute(CommandInput input, SimulationContext ctx) {
        return switch (input.getCommand()) {
            case "help", "?" -> buildHelp();
            case "status"    -> buildStatus(ctx);
            case "demo"      -> runDemo(input, ctx);
            case "version",
                 "info"      -> buildVersionInfo();
            default          -> buildHelp();
        };
    }

    private String buildHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(Formatter.section("Available Commands  —  Phase 2 Build"));

        sb.append(Formatter.bold("  Process Management\n"));
        sb.append("    ").append(Formatter.cyan("proc add <n> <arrival> <burst> [pri]")).append("\n");
        sb.append("    ").append(Formatter.cyan("proc list | proc rm <pid> | proc info <pid> | proc clear")).append("\n\n");

        sb.append(Formatter.bold("  Scheduling  (Phase 2: fcfs | sjf | rr | priority)\n"));
        sb.append("    ").append(Formatter.cyan("sched set <algo> [quantum]")).append("   e.g. sched set rr 4\n");
        sb.append("    ").append(Formatter.cyan("sched run")).append("  — run simulation\n");
        sb.append("    ").append(Formatter.cyan("sched list")).append(" — list algorithms\n\n");

        sb.append(Formatter.bold("  Memory  (Phase 2: first-fit only)\n"));
        sb.append("    ").append(Formatter.cyan("mem alloc <pid> <n> <size>")).append("\n");
        sb.append("    ").append(Formatter.cyan("mem free <pid> | mem map | mem init <size> | mem info")).append("\n\n");

        sb.append(Formatter.bold("  File System  (Phase 2: mkdir | touch | write | cat | rm | ls | cd | pwd)\n"));
        sb.append("    ").append(Formatter.cyan("pwd | cd | ls | mkdir | touch | write | cat | rm")).append("\n\n");

        sb.append(Formatter.yellow("  Phase 3 (coming next): srtf | priority-p | mlq | compare | paging\n"));
        sb.append(Formatter.yellow("                          mv | find | tree | append | batch scripting\n\n"));

        sb.append(Formatter.bold("  Other\n"));
        sb.append("    ").append(Formatter.cyan("status | demo | version | history | exit")).append("\n");
        return sb.toString();
    }

    private String buildStatus(SimulationContext ctx) {
        return String.format(
                "\n%s\n" +
                "  Phase            : 2 of 3  (70%% complete)\n" +
                "  Processes        : %d defined\n" +
                "  Active Scheduler : %s\n" +
                "  Memory           : %d / %d units  [First-Fit]\n" +
                "  Working Dir      : %s\n" +
                "  Phase 3 TODO     : SRTF, MLQ, Priority-P, Paging, Compare, Batch\n",
                Formatter.bold("─── System Status ────────────────────────────────────"),
                ctx.getProcessManager().size(),
                Formatter.cyan(ctx.getActiveScheduler().getName()),
                ctx.getMemoryManager().getUsedMemory(),
                ctx.getMemoryManager().getTotalMemory(),
                Formatter.yellow(ctx.getFileSystem().pwd()));
    }

    private String buildVersionInfo() {
        return "\n" + Formatter.bold("Mini OS Simulator") + " v0.7 (Phase 2)\n" +
               "  Done     : Process Management, FCFS/SJF/RR/Priority, First-Fit Memory, Basic VFS\n" +
               "  Phase 3  : SRTF, Priority-P, MLQ, IO-RR, Best-Fit, Worst-Fit, Paging, Compare\n";
    }

    private String runDemo(CommandInput input, SimulationContext ctx) {
        StringBuilder sb = new StringBuilder();
        ctx.getProcessManager().clear();
        sb.append(Formatter.yellow("Loading demo processes...\n"));
        ctx.getProcessManager().createProcess("Alpha",   0, 10, 3);
        ctx.getProcessManager().createProcess("Beta",    1,  4, 1);
        ctx.getProcessManager().createProcess("Gamma",   2,  6, 4);
        ctx.getProcessManager().getAll().forEach(p -> ctx.getProcessManager().admit(p));
        sb.append(Formatter.formatProcessList(ctx.getProcessManager().getAll()));

        for (String algo : new String[]{"fcfs", "sjf", "rr"}) {
            ctx.setScheduler(algo, 3);
            sb.append(Formatter.section("Algorithm: " + ctx.getActiveScheduler().getName()));
            try {
                var result = ctx.getActiveScheduler().simulate(ctx.getProcessManager().getAll());
                sb.append(Formatter.formatGantt(result));
                sb.append(Formatter.formatMetrics(result));
            } catch (Exception e) {
                sb.append(Formatter.red("  Error: " + e.getMessage() + "\n"));
            }
        }
        sb.append(Formatter.yellow("\n[Phase 3] SRTF, MLQ, Priority-P coming in next phase!\n"));
        return sb.toString();
    }

    @Override
    public String helpText() {
        return "help | status | demo | version";
    }
}
