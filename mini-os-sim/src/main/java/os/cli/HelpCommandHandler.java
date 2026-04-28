package os.cli;

import os.utils.CommandInput;
import os.utils.Formatter;

import java.util.List;

/**
 * Handles meta-commands: help, status, version, demo.
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
            case "status" -> buildStatus(ctx);
            case "demo" -> runDemo(ctx);
            case "version", "info" -> buildVersionInfo();
            default -> buildHelp();
        };
    }

    private String buildHelp() {
        StringBuilder sb = new StringBuilder();
        sb.append(Formatter.section("Available Commands"));

        sb.append(Formatter.bold("  Process Management\n"));
        sb.append("    ").append(Formatter.cyan("proc add <n> <arrival> <burst> [pri]")).append("\n");
        sb.append("    ").append(Formatter.cyan("proc list | proc info <pid> | proc start|pause|block|wake|term <pid>")).append("\n");
        sb.append("    ").append(Formatter.cyan("proc rm <pid> | proc clear")).append("\n\n");

        sb.append(Formatter.bold("  Scheduling\n"));
        sb.append("    ").append(Formatter.cyan("sched set <algo> [quantum]")).append("   e.g. sched set rr 4\n");
        sb.append("    ").append(Formatter.cyan("sched run")).append("  - run simulation on current processes\n");
        sb.append("    ").append(Formatter.cyan("sched list")).append(" - list supported algorithms\n\n");

        sb.append(Formatter.bold("  Memory\n"));
        sb.append("    ").append(Formatter.cyan("mem alloc <pid> <n> <size>")).append("\n");
        sb.append("    ").append(Formatter.cyan("mem free <pid> | mem map | mem init <size> [strategy] | mem info")).append("\n\n");

        sb.append(Formatter.bold("  File System\n"));
        sb.append("    ").append(Formatter.cyan("pwd | cd | ls | mkdir | touch | write | append | cat | rm | mv")).append("\n\n");

        sb.append(Formatter.bold("  IPC\n"));
        sb.append("    ").append(Formatter.cyan("ipc send <from> <to> <message...> | ipc recv <pid> | ipc inbox <pid>")).append("\n");
        sb.append("    ").append(Formatter.cyan("ipc mkchan <name> | ipc publish <channel> <from> <message...> | ipc readchan <name>")).append("\n\n");

        sb.append(Formatter.bold("  Other\n"));
        sb.append("    ").append(Formatter.cyan("status")).append("  - show system summary\n");
        sb.append("    ").append(Formatter.cyan("demo")).append("  - load sample processes and run\n");
        sb.append("    ").append(Formatter.cyan("exit | quit")).append("  - exit the simulator\n");
        return sb.toString();
    }

    private String buildStatus(SimulationContext ctx) {
        return String.format(
                "\n%s\n" +
                "  Processes        : %d defined\n" +
                "  Active Scheduler : %s\n" +
                "  Memory           : %d / %d units used  [%s]\n" +
                "  IPC Pending      : %d messages\n" +
                "  Working Dir      : %s\n",
                Formatter.bold("--- System Status ----------------------"),
                ctx.getProcessManager().size(),
                Formatter.cyan(ctx.getActiveScheduler().getName()),
                ctx.getMemoryManager().getUsedMemory(),
                ctx.getMemoryManager().getTotalMemory(),
                ctx.getMemStrategy().getDisplayName(),
                ctx.getIpcManager().getTotalPendingMessages(),
                Formatter.yellow(ctx.getFileSystem().pwd()));
    }

    private String buildVersionInfo() {
        return "\n" + Formatter.bold("Mini OS Simulator") + " v1.0\n" +
               "  Subsystems : Process Management (Java Threads), CPU Scheduling, Contiguous Memory, Virtual FS, IPC\n" +
               "  Algorithms : FCFS, SJF, SRTF, Round Robin\n" +
               "  Memory     : First-Fit, Best-Fit, Worst-Fit with coalescing\n" +
               "  FS         : Unix-style VFS (mkdir, touch, cat, rm, mv, ...)\n" +
               "  IPC        : Mailboxes + named channels built with Java concurrent queues\n";
    }

    private String runDemo(SimulationContext ctx) {
        StringBuilder sb = new StringBuilder();

        ctx.getProcessManager().getAll().forEach(p -> ctx.getIpcManager().unregisterProcess(p.getPid()));
        ctx.getProcessManager().clear();
        sb.append(Formatter.yellow("Loading demo processes...\n"));

        ctx.getProcessManager().createProcess("Alpha",   0, 10, 3);
        ctx.getProcessManager().createProcess("Beta",    1,  4, 1);
        ctx.getProcessManager().createProcess("Gamma",   2,  6, 4);
        ctx.getProcessManager().createProcess("Delta",   3,  8, 2);
        ctx.getProcessManager().createProcess("Epsilon", 4,  2, 5);

        ctx.getProcessManager().getAll().forEach(p -> {
            ctx.getProcessManager().admit(p);
            ctx.getIpcManager().registerProcess(p.getPid());
        });
        sb.append(Formatter.formatProcessList(ctx.getProcessManager().getAll()));

        String[] algos = {"fcfs", "sjf", "srtf", "rr"};
        for (String algo : algos) {
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
        return sb.toString();
    }

    @Override
    public String helpText() {
        return "help | status | demo | version";
    }
}
