package os.memory;

/**
 * Represents a single contiguous block of physical memory.
 *
 * A block is either free (allocated == false, pid == -1) or allocated to
 * exactly one process.  The memory manager maintains an ordered list of
 * non-overlapping blocks that together span the entire memory address space.
 */
public class MemoryBlock {

    private final int startAddress;
    private int       size;          // bytes / units
    private boolean   allocated;
    private int       pid;           // -1 when free
    private String    processName;   // empty when free

    public MemoryBlock(int startAddress, int size) {
        this.startAddress = startAddress;
        this.size         = size;
        this.allocated    = false;
        this.pid          = -1;
        this.processName  = "";
    }

    // ── Copy constructor ─────────────────────────────────────────────────────
    public MemoryBlock(MemoryBlock other) {
        this.startAddress = other.startAddress;
        this.size         = other.size;
        this.allocated    = other.allocated;
        this.pid          = other.pid;
        this.processName  = other.processName;
    }

    // ── Allocation helpers ───────────────────────────────────────────────────

    public void allocate(int pid, String processName) {
        this.allocated   = true;
        this.pid         = pid;
        this.processName = processName;
    }

    public void free() {
        this.allocated   = false;
        this.pid         = -1;
        this.processName = "";
    }

    // ── Getters / setters ────────────────────────────────────────────────────

    public int     getStartAddress()  { return startAddress; }
    public int     getEndAddress()    { return startAddress + size - 1; }
    public int     getSize()          { return size; }
    public void    setSize(int s)     { this.size = s; }
    public boolean isAllocated()      { return allocated; }
    public int     getPid()           { return pid; }
    public String  getProcessName()   { return processName; }

    @Override
    public String toString() {
        if (allocated)
            return String.format("[%4d - %4d | %4d u | P%-2d %-10s]",
                    startAddress, getEndAddress(), size, pid, processName);
        else
            return String.format("[%4d - %4d | %4d u | FREE          ]",
                    startAddress, getEndAddress(), size);
    }
}
