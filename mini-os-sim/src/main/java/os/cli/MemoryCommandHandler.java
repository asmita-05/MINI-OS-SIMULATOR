package os.cli;

import os.memory.AllocationStrategy;
import os.memory.MemoryManager;
import os.utils.CommandInput;
import os.utils.Formatter;

/**
 * Handles memory-management commands:
 *
 *   mem alloc <pid> <name> <size>   — allocate memory to a process
 *   mem free  <pid>                 — free memory held by a process
 *   mem map                         — display memory map
 *   mem init  <size> [strategy]     — reinitialise memory with a new size/strategy
 *   mem info                        — show current strategy and free/used stats
 *   mem compact                     — not yet implemented (placeholder)
 */
public class MemoryCommandHandler implements CommandHandler {

    @Override
    public boolean handles(String command) {
        return command.equals("mem") || command.equals("memory");
    }

    @Override
    public String execute(CommandInput input, SimulationContext ctx) {
        String sub = input.getArg(0).toLowerCase();
        return switch (sub) {
            case "alloc", "allocate"   -> handleAlloc(input, ctx);
            case "free", "dealloc"     -> handleFree(input, ctx);
            case "map", "show", "ls"   -> handleMap(ctx);
            case "init", "reinit"      -> handleInit(input, ctx);
            case "info", "status"      -> handleInfo(ctx);
            default                    -> Formatter.red(
                    "Unknown sub-command '" + sub + "'. Use: alloc | free | map | init | info\n");
        };
    }

    // ── sub-commands ────────────────────────────────────────────────────────

    private String handleAlloc(CommandInput input, SimulationContext ctx) {
        // mem alloc <pid> <name> <size>
        if (input.getArgCount() < 4)
            return Formatter.red("Usage: mem alloc <pid> <processName> <size>\n");

        int    pid  = input.getIntArg(1, -1);
        String name = input.getArg(2);
        int    size = input.getIntArg(3, -1);

        if (pid < 0 || size < 1)
            return Formatter.red("Invalid PID or size. PID > 0, size >= 1.\n");

        int start = ctx.getMemoryManager().allocate(pid, name, size);
        if (start < 0)
            return Formatter.red("Allocation failed — insufficient contiguous memory for " + size + " units.\n");

        // Update process record if process exists
        ctx.getProcessManager().findByPid(pid).ifPresent(p -> {
            p.setMemoryStart(start);
            p.setMemorySize(size);
        });

        return Formatter.green(String.format("✓ Allocated %d units for P%d ('%s') at address %d.\n",
                size, pid, name, start));
    }

    private String handleFree(CommandInput input, SimulationContext ctx) {
        // mem free <pid>
        if (input.getArgCount() < 2)
            return Formatter.red("Usage: mem free <pid>\n");

        int pid = input.getIntArg(1, -1);
        if (pid < 0) return Formatter.red("Invalid PID.\n");

        boolean freed = ctx.getMemoryManager().free(pid);
        if (freed) {
            ctx.getProcessManager().findByPid(pid).ifPresent(p -> {
                p.setMemoryStart(-1);
                p.setMemorySize(0);
            });
            return Formatter.green("✓ Memory freed for P" + pid + ".\n");
        }
        return Formatter.yellow("No memory allocated for P" + pid + ".\n");
    }

    private String handleMap(SimulationContext ctx) {
        MemoryManager mm = ctx.getMemoryManager();
        return Formatter.formatMemoryMap(mm.getMemoryMap(), mm.getTotalMemory());
    }

    private String handleInit(CommandInput input, SimulationContext ctx) {
        // mem init <size> [strategy: first-fit | best-fit | worst-fit]
        if (input.getArgCount() < 2)
            return Formatter.red("Usage: mem init <size> [first-fit|best-fit|worst-fit]\n");

        int size = input.getIntArg(1, -1);
        if (size < 1) return Formatter.red("Memory size must be >= 1.\n");

        AllocationStrategy strat;
        try {
            strat = input.getArgCount() >= 3
                    ? AllocationStrategy.fromString(input.getArg(2))
                    : ctx.getMemStrategy();
        } catch (IllegalArgumentException e) {
            return Formatter.red(e.getMessage() + "\n");
        }

        ctx.reinitMemory(size, strat);
        return Formatter.green(String.format(
                "✓ Memory reinitialised: %d units, strategy=%s.\n", size, strat.getDisplayName()));
    }

    private String handleInfo(SimulationContext ctx) {
        MemoryManager mm = ctx.getMemoryManager();
        return String.format(
                "\n%s\n" +
                "  Strategy    : %s\n" +
                "  Total       : %d units\n" +
                "  Used        : %d units\n" +
                "  Free        : %d units\n" +
                "  Utilization : %.1f%%\n",
                Formatter.bold("─── Memory Info ──────────────────────────────────────"),
                Formatter.cyan(ctx.getMemStrategy().getDisplayName()),
                mm.getTotalMemory(), mm.getUsedMemory(), mm.getFreeMemory(),
                100.0 * mm.getUsedMemory() / mm.getTotalMemory());
    }

    @Override
    public String helpText() {
        return "mem alloc <pid> <n> <size>  |  mem free <pid>  |  mem map  |  mem init <size> [strategy]  |  mem info";
    }
}
