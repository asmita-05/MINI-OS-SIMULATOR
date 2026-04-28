package os.memory;

import java.util.*;

/**
 * Paging-based memory manager.
 *
 * Physical memory is divided into fixed-size frames; each process's logical
 * address space is divided into same-size pages.  There is no external
 * fragmentation (every frame is fully usable), but internal fragmentation
 * may occur in the last page of each allocation.
 *
 * This implementation models:
 *   - Frame allocation / deallocation
 *   - Per-process page tables (logical → physical mapping)
 *   - Address translation: logical address → physical address
 *   - Page-fault detection (accessing an unmapped page)
 *
 * Note: This is a conceptual simulation.  There is no actual backing store
 * or swap mechanism — "page faults" are modelled as allocation failures.
 */
public class PagingMemoryManager implements MemoryManager {

    private final int totalMemory;
    private final int pageSize;
    private final int totalFrames;

    /** Bit-set tracking which frames are free (true = free). */
    private final boolean[] freeFrames;

    /** pid → list of PageTableEntries for that process */
    private final Map<Integer, List<PageTableEntry>> pageTables;

    /** pid → process name (for display) */
    private final Map<Integer, String> processNames;

    public PagingMemoryManager(int totalMemory, int pageSize) {
        if (totalMemory < pageSize)
            throw new IllegalArgumentException("Total memory must be >= page size.");
        if (pageSize < 1 || (pageSize & (pageSize - 1)) != 0)
            throw new IllegalArgumentException("Page size must be a power of 2 and >= 1.");

        this.totalMemory  = totalMemory;
        this.pageSize     = pageSize;
        this.totalFrames  = totalMemory / pageSize;
        this.freeFrames   = new boolean[totalFrames];
        this.pageTables   = new LinkedHashMap<>();
        this.processNames = new HashMap<>();

        Arrays.fill(freeFrames, true); // all frames initially free
    }

    // ── MemoryManager contract ───────────────────────────────────────────────

    /**
     * Allocates enough frames to hold {@code size} bytes for process {@code pid}.
     *
     * Pages required = ⌈size / pageSize⌉.
     *
     * @return logical base address 0 on success, -1 if insufficient frames.
     */
    @Override
    public int allocate(int pid, String processName, int size) {
        int pagesNeeded = (int) Math.ceil((double) size / pageSize);
        List<Integer> availableFrames = findFreeFrames(pagesNeeded);
        if (availableFrames == null) return -1;

        // Build page table for this process
        List<PageTableEntry> pageTable = pageTables.computeIfAbsent(pid, k -> new ArrayList<>());
        int startPage = pageTable.size(); // next logical page index

        for (int i = 0; i < pagesNeeded; i++) {
            int frame = availableFrames.get(i);
            freeFrames[frame] = false;
            PageTableEntry pte = new PageTableEntry(startPage + i);
            pte.map(frame);
            pageTable.add(pte);
        }

        processNames.put(pid, processName);
        // Logical addresses always start at 0 for the process's own view
        return 0;
    }

    /** Frees all frames allocated to process {@code pid}. */
    @Override
    public boolean free(int pid) {
        List<PageTableEntry> pageTable = pageTables.remove(pid);
        processNames.remove(pid);
        if (pageTable == null || pageTable.isEmpty()) return false;
        for (PageTableEntry pte : pageTable) {
            if (pte.isValid()) {
                freeFrames[pte.getFrameNumber()] = true;
                pte.unmap();
            }
        }
        return true;
    }

    /**
     * Returns memory map as a list of pseudo-MemoryBlock objects,
     * one per physical frame, for display compatibility with the CLI.
     */
    @Override
    public List<MemoryBlock> getMemoryMap() {
        // Build a reverse map: frame → (pid, pageName)
        int[]    framePid  = new int[totalFrames];
        String[] frameName = new String[totalFrames];
        Arrays.fill(framePid, -1);

        for (Map.Entry<Integer, List<PageTableEntry>> e : pageTables.entrySet()) {
            int pid = e.getKey();
            for (PageTableEntry pte : e.getValue()) {
                if (pte.isValid()) {
                    framePid [pte.getFrameNumber()] = pid;
                    frameName[pte.getFrameNumber()] = processNames.getOrDefault(pid, "?")
                            + "[p" + pte.getPageNumber() + "]";
                }
            }
        }

        List<MemoryBlock> blocks = new ArrayList<>(totalFrames);
        for (int f = 0; f < totalFrames; f++) {
            MemoryBlock mb = new MemoryBlock(f * pageSize, pageSize);
            if (framePid[f] >= 0) mb.allocate(framePid[f], frameName[f]);
            blocks.add(mb);
        }
        return Collections.unmodifiableList(blocks);
    }

    // ── Address translation ──────────────────────────────────────────────────

    /**
     * Translates a logical address to a physical address for process {@code pid}.
     *
     * @return physical address, or -1 on page fault (page not mapped)
     */
    public int translateAddress(int pid, int logicalAddress) {
        List<PageTableEntry> pageTable = pageTables.get(pid);
        if (pageTable == null) return -1;

        int pageNumber = logicalAddress / pageSize;
        int offset     = logicalAddress % pageSize;

        if (pageNumber >= pageTable.size()) return -1; // page fault
        PageTableEntry pte = pageTable.get(pageNumber);
        if (!pte.isValid()) return -1;

        pte.setReferenced(true);
        return pte.translateAddress(offset, pageSize);
    }

    // ── Display helpers ──────────────────────────────────────────────────────

    /** Returns the page table for a given process (read-only view). */
    public List<PageTableEntry> getPageTable(int pid) {
        return pageTables.getOrDefault(pid, Collections.emptyList());
    }

    public String formatPageTable(int pid) {
        List<PageTableEntry> pt = getPageTable(pid);
        if (pt.isEmpty()) return "  (no page table for PID " + pid + ")\n";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Page Table for P%d (%s):\n", pid,
                processNames.getOrDefault(pid, "?")));
        sb.append(String.format("  %-8s %-8s %-6s\n", "Page#", "Frame#", "Flags"));
        sb.append("  " + "─".repeat(30) + "\n");
        for (PageTableEntry pte : pt)
            sb.append("  ").append(pte).append("\n");
        return sb.toString();
    }

    // ── Stats ────────────────────────────────────────────────────────────────

    @Override
    public int getTotalMemory() { return totalMemory; }

    @Override
    public int getFreeMemory() {
        int free = 0;
        for (boolean f : freeFrames) if (f) free++;
        return free * pageSize;
    }

    @Override
    public int getUsedMemory() { return totalMemory - getFreeMemory(); }

    @Override
    public void reset() {
        Arrays.fill(freeFrames, true);
        pageTables.clear();
        processNames.clear();
    }

    public int getPageSize()    { return pageSize; }
    public int getTotalFrames() { return totalFrames; }
    public int getFreeFrames()  { return getFreeMemory() / pageSize; }

    // ── Private helpers ──────────────────────────────────────────────────────

    /** Returns a list of {@code count} free frame indices, or null if unavailable. */
    private List<Integer> findFreeFrames(int count) {
        List<Integer> frames = new ArrayList<>(count);
        for (int i = 0; i < totalFrames && frames.size() < count; i++)
            if (freeFrames[i]) frames.add(i);
        return frames.size() == count ? frames : null;
    }
}
