package os.cli;

import os.memory.AllocationStrategy;
import os.process.Process;
import os.scheduler.ScheduleResult;
import os.utils.CommandInput;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the CLI handler pipeline.
 *
 * Tests drive the full command dispatch chain:
 *   CommandInput → CommandHandler.execute() → SimulationContext
 *
 * This validates that the handlers parse arguments correctly, invoke the
 * right subsystems, and produce the expected state changes.
 */
class CLIIntegrationTest {

    private SimulationContext ctx;
    private ProcessCommandHandler   procHandler;
    private SchedulerCommandHandler schedHandler;
    private MemoryCommandHandler    memHandler;

    @BeforeEach
    void setUp() {
        ctx         = new SimulationContext();
        procHandler  = new ProcessCommandHandler();
        schedHandler = new SchedulerCommandHandler();
        memHandler   = new MemoryCommandHandler();
    }

    // ── Process command handler ───────────────────────────────────────────────

    @Test
    void procAdd_createsProcess() {
        String out = procHandler.execute(cmd("proc add Alpha 0 10 2"), ctx);
        assertTrue(out.contains("✓"), "Should confirm creation");
        assertEquals(1, ctx.getProcessManager().size());
        Process p = ctx.getProcessManager().getAll().get(0);
        assertEquals("Alpha", p.getName());
        assertEquals(0, p.getArrivalTime());
        assertEquals(10, p.getBurstTime());
        assertEquals(2, p.getPriority());
    }

    @Test
    void procAdd_invalidBurst_reportsError() {
        String out = procHandler.execute(cmd("proc add Bad 0 0 0"), ctx);
        assertTrue(out.toLowerCase().contains("error") || out.contains("Invalid"),
                "Should report error for zero burst");
        assertEquals(0, ctx.getProcessManager().size());
    }

    @Test
    void procList_emptyMessage_whenNoProcesses() {
        String out = procHandler.execute(cmd("proc list"), ctx);
        assertTrue(out.contains("no processes") || out.contains("empty") || out.contains("("),
                "Should indicate empty process list");
    }

    @Test
    void procList_showsAllProcesses() {
        procHandler.execute(cmd("proc add P1 0 5 0"), ctx);
        procHandler.execute(cmd("proc add P2 1 3 1"), ctx);
        String out = procHandler.execute(cmd("proc list"), ctx);
        assertTrue(out.contains("P1") && out.contains("P2"));
    }

    @Test
    void procRm_removesProcess() {
        procHandler.execute(cmd("proc add P1 0 5 0"), ctx);
        int pid = ctx.getProcessManager().getAll().get(0).getPid();
        String out = procHandler.execute(cmd("proc rm " + pid), ctx);
        assertTrue(out.contains("✓"));
        assertEquals(0, ctx.getProcessManager().size());
    }

    @Test
    void procRm_unknownPid_reportsError() {
        String out = procHandler.execute(cmd("proc rm 999"), ctx);
        assertTrue(out.contains("not found") || out.toLowerCase().contains("error"));
    }

    @Test
    void procClear_removesAll() {
        procHandler.execute(cmd("proc add A 0 3 0"), ctx);
        procHandler.execute(cmd("proc add B 1 4 0"), ctx);
        procHandler.execute(cmd("proc clear"), ctx);
        assertEquals(0, ctx.getProcessManager().size());
    }

    // ── Scheduler command handler ─────────────────────────────────────────────

    @Test
    void schedSet_changesActiveAlgorithm() {
        schedHandler.execute(cmd("sched set sjf"), ctx);
        assertTrue(ctx.getActiveScheduler().getName().toLowerCase().contains("sjf"));
    }

    @Test
    void schedSet_roundRobin_withQuantum() {
        schedHandler.execute(cmd("sched set rr 5"), ctx);
        assertTrue(ctx.getActiveScheduler().getName().contains("5"));
    }

    @Test
    void schedSet_unknownAlgo_reportsError() {
        String out = schedHandler.execute(cmd("sched set foobar"), ctx);
        assertTrue(out.toLowerCase().contains("error") || out.toLowerCase().contains("unknown"));
    }

    @Test
    void schedRun_withProcesses_producesOutput() {
        addClassicProcesses();
        schedHandler.execute(cmd("sched set fcfs"), ctx);
        String out = schedHandler.execute(cmd("sched run"), ctx);
        assertTrue(out.contains("Gantt") || out.contains("P1"), "Should contain gantt output");
        assertTrue(out.contains("Avg") || out.contains("WT"), "Should contain metrics");
    }

    @Test
    void schedRun_noProcesses_reportsWarning() {
        String out = schedHandler.execute(cmd("sched run"), ctx);
        assertTrue(out.contains("No processes") || out.toLowerCase().contains("empty"));
    }

    @Test
    void schedRun_allAlgorithms_doNotThrow() {
        addClassicProcesses();
        String[] algos = {"fcfs", "sjf", "srtf", "rr", "priority", "priority-p", "mlq"};
        for (String algo : algos) {
            schedHandler.execute(cmd("sched set " + algo + " 3"), ctx);
            String out = schedHandler.execute(cmd("sched run"), ctx);
            assertFalse(out.contains("[ERROR]"),
                    "Algorithm " + algo + " threw an error: " + out);
        }
    }

    // ── Memory command handler ────────────────────────────────────────────────

    @Test
    void memInit_setsCorrectSize() {
        memHandler.execute(cmd("mem init 512 first-fit"), ctx);
        assertEquals(512, ctx.getMemoryManager().getTotalMemory());
        assertEquals(AllocationStrategy.FIRST_FIT, ctx.getMemStrategy());
    }

    @Test
    void memInit_bestFit() {
        memHandler.execute(cmd("mem init 1024 best-fit"), ctx);
        assertEquals(AllocationStrategy.BEST_FIT, ctx.getMemStrategy());
    }

    @Test
    void memAlloc_allocatesCorrectly() {
        memHandler.execute(cmd("mem init 512 first-fit"), ctx);
        String out = memHandler.execute(cmd("mem alloc 1 Alpha 128"), ctx);
        assertTrue(out.contains("✓"));
        assertEquals(128, ctx.getMemoryManager().getUsedMemory());
    }

    @Test
    void memFree_releasesMemory() {
        memHandler.execute(cmd("mem init 256 first-fit"), ctx);
        memHandler.execute(cmd("mem alloc 1 Alpha 128"), ctx);
        String out = memHandler.execute(cmd("mem free 1"), ctx);
        assertTrue(out.contains("✓"));
        assertEquals(256, ctx.getMemoryManager().getFreeMemory());
    }

    @Test
    void memMap_showsBlocks() {
        memHandler.execute(cmd("mem alloc 1 P1 64"), ctx);
        String out = memHandler.execute(cmd("mem map"), ctx);
        assertTrue(out.contains("P1") || out.contains("USED"));
        assertTrue(out.contains("FREE") || out.contains("free"));
    }

    @Test
    void memAuto_enablesAutoAllocation() {
        memHandler.execute(cmd("mem auto on 64"), ctx);
        assertTrue(ctx.isAutoAllocMemory());
        assertEquals(64, ctx.getDefaultMemoryPerProc());
    }

    // ── Full pipeline (process + schedule + memory) ────────────────────────────

    @Test
    void fullPipeline_createScheduleAllocate() {
        // 1. Initialise memory
        memHandler.execute(cmd("mem init 512 best-fit"), ctx);

        // 2. Create processes with auto-memory
        memHandler.execute(cmd("mem auto on 64"), ctx);
        procHandler.execute(cmd("proc add P1 0 10 2"), ctx);
        procHandler.execute(cmd("proc add P2 1  4 1"), ctx);
        procHandler.execute(cmd("proc add P3 2  6 3"), ctx);

        assertEquals(3, ctx.getProcessManager().size());
        assertEquals(192, ctx.getMemoryManager().getUsedMemory()); // 3 × 64

        // 3. Schedule
        schedHandler.execute(cmd("sched set rr 4"), ctx);
        String out = schedHandler.execute(cmd("sched run"), ctx);
        assertFalse(out.contains("[ERROR]"), "Simulation should not error");
        assertTrue(out.contains("P1") && out.contains("P2") && out.contains("P3"));

        // 4. Verify all processes completed with valid metrics
        List<Process> procs = ctx.getProcessManager().getAll();
        ScheduleResult result = ctx.getActiveScheduler().simulate(procs);
        assertTrue(result.getAvgWaitingTime() >= 0);
        assertTrue(result.getCpuUtilization() > 0 && result.getCpuUtilization() <= 1.0);
    }

    @Test
    void compare_runsAllAlgorithmsWithoutError() {
        addClassicProcesses();
        CompareCommandHandler cmp = new CompareCommandHandler();
        String out = cmp.execute(cmd("compare"), ctx);
        assertFalse(out.contains("[ERROR]"), "Compare should not error");
        assertTrue(out.contains("Algorithm") || out.contains("FCFS"));
    }

    @Test
    void compare_subsetOfAlgorithms() {
        addClassicProcesses();
        CompareCommandHandler cmp = new CompareCommandHandler();
        String out = cmp.execute(cmd("compare fcfs sjf"), ctx);
        assertTrue(out.contains("First Come") || out.contains("FCFS"));
        assertTrue(out.contains("Shortest") || out.contains("SJF"));
    }

    // ── FileSystem integration ────────────────────────────────────────────────

    @Test
    void fileSystem_createAndReadFile() {
        FileSystemCommandHandler fsHandler = new FileSystemCommandHandler();
        fsHandler.execute(cmd("mkdir /workspace"), ctx);
        fsHandler.execute(cmd("write /workspace/notes.txt hello simulator"), ctx);
        String out = fsHandler.execute(cmd("cat /workspace/notes.txt"), ctx);
        assertTrue(out.contains("hello simulator"));
    }

    @Test
    void fileSystem_tree_showsHierarchy() {
        FileSystemCommandHandler fsHandler = new FileSystemCommandHandler();
        fsHandler.execute(cmd("mkdir /project"), ctx);
        fsHandler.execute(cmd("mkdir /project/src"), ctx);
        fsHandler.execute(cmd("touch /project/src/Main.java"), ctx);
        String out = fsHandler.execute(cmd("tree /project"), ctx);
        assertTrue(out.contains("src") && out.contains("Main.java"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CommandInput cmd(String line) { return new CommandInput(line); }

    private void addClassicProcesses() {
        procHandler.execute(cmd("proc add Alpha   0 10 3"), ctx);
        procHandler.execute(cmd("proc add Beta    1  4 1"), ctx);
        procHandler.execute(cmd("proc add Gamma   2  6 4"), ctx);
        procHandler.execute(cmd("proc add Delta   3  8 2"), ctx);
        procHandler.execute(cmd("proc add Epsilon 4  2 5"), ctx);
    }
}
