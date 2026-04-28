package os.cli;

import os.memory.PagingMemoryManager;
import os.utils.CommandInput;
import os.utils.Formatter;

/**
 * Handles paging-specific memory commands (only available when the active
 * memory manager is a {@link PagingMemoryManager}).
 *
 *   page init <totalMem> <pageSize>   — switch to paging memory model
 *   page table <pid>                  — print page table for a process
 *   page frames                       — print physical frame allocation
 *   page translate <pid> <logAddr>    — translate logical → physical address
 */
public class PagingCommandHandler implements CommandHandler {

    @Override
    public boolean handles(String command) {
        return command.equals("page") || command.equals("paging");
    }

    @Override
    public String execute(CommandInput input, SimulationContext ctx) {
        String sub = input.getArg(0).toLowerCase();
        return switch (sub) {
            case "init"               -> handleInit(input, ctx);
            case "table"              -> handleTable(input, ctx);
            case "frames", "map"      -> handleFrames(ctx);
            case "translate", "trans" -> handleTranslate(input, ctx);
            default                   -> Formatter.red(
                    "Unknown sub-command '" + sub + "'. Use: init | table | frames | translate\n");
        };
    }

    private String handleInit(CommandInput input, SimulationContext ctx) {
        if (input.getArgCount() < 3)
            return Formatter.red("Usage: page init <totalMemory> <pageSize>\n");

        int total    = input.getIntArg(1, -1);
        int pageSize = input.getIntArg(2, -1);

        if (total < 1 || pageSize < 1)
            return Formatter.red("Both totalMemory and pageSize must be >= 1.\n");
        if (total % pageSize != 0)
            return Formatter.red("totalMemory must be an exact multiple of pageSize.\n");

        try {
            PagingMemoryManager pmm = new PagingMemoryManager(total, pageSize);
            ctx.reinitMemory(total, ctx.getMemStrategy()); // update size in context
            // Replace the manager directly via the context's reinit mechanism
            // (we piggyback on reinitMemory to keep total in sync)
            // For a cleaner design we keep paging as a separate manager instance:
            storePageManager(ctx, pmm);
            return Formatter.green(String.format(
                    "✓ Paging enabled: %d units total, page size=%d, frames=%d.\n",
                    total, pageSize, total / pageSize));
        } catch (IllegalArgumentException e) {
            return Formatter.red("Error: " + e.getMessage() + "\n");
        }
    }

    private String handleTable(CommandInput input, SimulationContext ctx) {
        PagingMemoryManager pmm = getPageManager(ctx);
        if (pmm == null) return notPaging();

        if (input.getArgCount() < 2) {
            // Print all page tables
            StringBuilder sb = new StringBuilder(Formatter.section("Page Tables"));
            ctx.getProcessManager().getAll().forEach(p ->
                    sb.append(pmm.formatPageTable(p.getPid())));
            return sb.toString();
        }

        int pid = input.getIntArg(1, -1);
        if (pid < 0) return Formatter.red("Invalid PID.\n");
        return Formatter.section("Page Table — P" + pid) + pmm.formatPageTable(pid);
    }

    private String handleFrames(SimulationContext ctx) {
        PagingMemoryManager pmm = getPageManager(ctx);
        if (pmm == null) return notPaging();

        StringBuilder sb = new StringBuilder(Formatter.section("Physical Frame Map"));
        sb.append(String.format("  Page size: %d u | Total frames: %d | Free: %d | Used: %d\n\n",
                pmm.getPageSize(), pmm.getTotalFrames(),
                pmm.getFreeFrames(), pmm.getTotalFrames() - pmm.getFreeFrames()));

        var blocks = pmm.getMemoryMap();
        sb.append(String.format("  %-6s %-10s %-10s %s\n", "Frame", "Addr Range", "PID", "Status"));
        sb.append("  " + "─".repeat(40) + "\n");

        for (int i = 0; i < blocks.size(); i++) {
            var b = blocks.get(i);
            String status = b.isAllocated()
                    ? Formatter.cyan("P" + b.getPid())
                    : Formatter.yellow("FREE");
            sb.append(String.format("  %-6d %-10s %-10s %s\n",
                    i,
                    b.getStartAddress() + "-" + b.getEndAddress(),
                    b.isAllocated() ? String.valueOf(b.getPid()) : "-",
                    status));
        }
        return sb.toString();
    }

    private String handleTranslate(CommandInput input, SimulationContext ctx) {
        PagingMemoryManager pmm = getPageManager(ctx);
        if (pmm == null) return notPaging();

        if (input.getArgCount() < 3)
            return Formatter.red("Usage: page translate <pid> <logicalAddress>\n");

        int pid      = input.getIntArg(1, -1);
        int logAddr  = input.getIntArg(2, -1);
        if (pid < 0 || logAddr < 0) return Formatter.red("Invalid PID or logical address.\n");

        int pageSize = pmm.getPageSize();
        int pageNum  = logAddr / pageSize;
        int offset   = logAddr % pageSize;

        var pageTable = pmm.getPageTable(pid);
        if (pageTable.isEmpty())
            return Formatter.red("No pages allocated for P" + pid + ".\n");
        if (pageNum >= pageTable.size())
            return Formatter.red(String.format(
                    "Page fault! Logical address %d → page %d is out of range (allocated pages: %d).\n",
                    logAddr, pageNum, pageTable.size()));

        var pte      = pageTable.get(pageNum);
        int physAddr = pte.getFrameNumber() * pageSize + offset;

        return String.format(
                "\n  Logical Address  : %d\n" +
                "  Page Number      : %d\n" +
                "  Offset           : %d\n" +
                "  Physical Frame   : %d\n" +
                "  Physical Address : %s\n",
                logAddr, pageNum, offset,
                pte.getFrameNumber(),
                Formatter.green(String.valueOf(physAddr)));
    }

    // ── Context helpers (store paging manager in context) ──────────────────

    private static final String PAGING_KEY = "__pagingManager";

    private void storePageManager(SimulationContext ctx, PagingMemoryManager pmm) {
        // We store it via a small side-channel: reinit with same size so the
        // context total is consistent, then keep the pmm alive in a thread-local.
        // For a full design the SimulationContext would expose a setPagingManager().
        pagingManagerHolder = pmm;
    }

    private PagingMemoryManager getPageManager(SimulationContext ctx) {
        return pagingManagerHolder;
    }

    // Package-scoped static holder (simple approach; real design uses DI)
    static PagingMemoryManager pagingManagerHolder = null;

    private String notPaging() {
        return Formatter.yellow(
                "Paging is not active. Use 'page init <totalMem> <pageSize>' to enable it.\n");
    }

    @Override
    public String helpText() {
        return "page init <size> <pageSize>  |  page table [pid]  |  page frames  |  page translate <pid> <addr>";
    }
}
