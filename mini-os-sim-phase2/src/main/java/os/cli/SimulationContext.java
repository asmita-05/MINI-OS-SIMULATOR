package os.cli;

import os.memory.AllocationStrategy;
import os.memory.ContiguousMemoryManager;
import os.memory.MemoryManager;
import os.process.ProcessManager;
import os.filesystem.VirtualFileSystem;
import os.scheduler.Scheduler;
import os.scheduler.SchedulerFactory;

/**
 * Central context object holding all active subsystem instances.
 *
 * A single {@code SimulationContext} is created at startup and passed to
 * every command handler, providing a clean alternative to static globals.
 * Subsystem state (processes, memory, filesystem, scheduler choice) is
 * all accessible and mutable through this object.
 */
public class SimulationContext {

    // ── Subsystems ───────────────────────────────────────────────────────────
    private final ProcessManager   processManager;
    private       MemoryManager    memoryManager;
    private final VirtualFileSystem fileSystem;

    // ── Active scheduler ────────────────────────────────────────────────────
    private Scheduler activeScheduler;

    // ── Session config ───────────────────────────────────────────────────────
    private int  defaultQuantum         = 4;
    private int  defaultMemorySize      = 1024;
    private AllocationStrategy memStrategy = AllocationStrategy.FIRST_FIT;
    private boolean autoAllocMemory     = false;   // allocate memory on process create
    private int  defaultMemoryPerProc   = 64;

    public SimulationContext() {
        this.processManager = new ProcessManager();
        this.memoryManager  = new ContiguousMemoryManager(defaultMemorySize, memStrategy);
        this.fileSystem     = new VirtualFileSystem();
        this.activeScheduler = SchedulerFactory.create("fcfs");
    }

    // ── Scheduler ────────────────────────────────────────────────────────────

    public void setScheduler(String key, int quantum) {
        this.activeScheduler = SchedulerFactory.create(key, quantum);
        this.defaultQuantum  = quantum;
    }

    public Scheduler getActiveScheduler() { return activeScheduler; }

    // ── Memory ───────────────────────────────────────────────────────────────

    public void reinitMemory(int size, AllocationStrategy strategy) {
        this.defaultMemorySize = size;
        this.memStrategy       = strategy;
        this.memoryManager     = new ContiguousMemoryManager(size, strategy);
    }

    public MemoryManager    getMemoryManager()  { return memoryManager; }
    public AllocationStrategy getMemStrategy()  { return memStrategy; }
    public int getDefaultMemorySize()           { return defaultMemorySize; }

    // ── Process ──────────────────────────────────────────────────────────────

    public ProcessManager getProcessManager()   { return processManager; }

    // ── FileSystem ───────────────────────────────────────────────────────────

    public VirtualFileSystem getFileSystem()    { return fileSystem; }

    // ── Config ───────────────────────────────────────────────────────────────

    public int     getDefaultQuantum()             { return defaultQuantum; }
    public void    setDefaultQuantum(int q)        { this.defaultQuantum = q; }
    public boolean isAutoAllocMemory()             { return autoAllocMemory; }
    public void    setAutoAllocMemory(boolean v)   { this.autoAllocMemory = v; }
    public int     getDefaultMemoryPerProc()       { return defaultMemoryPerProc; }
    public void    setDefaultMemoryPerProc(int m)  { this.defaultMemoryPerProc = m; }
}
