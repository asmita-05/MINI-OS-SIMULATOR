package os.memory;

/**
 * Represents a single entry in a process page table.
 *
 * Maps one logical page number → one physical frame number.
 * A valid bit tracks whether the page is currently resident in memory
 * (present) or has been swapped out (absent).
 */
public class PageTableEntry {

    private final int     pageNumber;   // logical page index
    private       int     frameNumber;  // physical frame index (-1 = not allocated)
    private       boolean valid;        // present in physical memory?
    private       boolean dirty;        // page has been modified?
    private       boolean referenced;   // page has been accessed recently?

    public PageTableEntry(int pageNumber) {
        this.pageNumber  = pageNumber;
        this.frameNumber = -1;
        this.valid       = false;
        this.dirty       = false;
        this.referenced  = false;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public int     getPageNumber()           { return pageNumber; }
    public int     getFrameNumber()          { return frameNumber; }
    public boolean isValid()                 { return valid; }
    public boolean isDirty()                 { return dirty; }
    public boolean isReferenced()            { return referenced; }

    public void setFrameNumber(int fn)       { this.frameNumber = fn; }
    public void setValid(boolean v)          { this.valid = v; }
    public void setDirty(boolean d)          { this.dirty = d; }
    public void setReferenced(boolean r)     { this.referenced = r; }

    /** Marks this page as mapped to {@code frameNumber}. */
    public void map(int frameNumber) {
        this.frameNumber = frameNumber;
        this.valid       = true;
        this.referenced  = true;
    }

    /** Invalidates the mapping (simulates a page eviction). */
    public void unmap() {
        this.frameNumber = -1;
        this.valid       = false;
    }

    /**
     * Translates a logical address offset within this page to a physical address.
     *
     * @param offset    byte offset within the page
     * @param pageSize  system page size in bytes
     * @return physical address, or -1 if the page is not mapped
     */
    public int translateAddress(int offset, int pageSize) {
        if (!valid) return -1;
        return frameNumber * pageSize + offset;
    }

    @Override
    public String toString() {
        return String.format("P%-3d → F%-3d [%s%s%s]",
                pageNumber, frameNumber,
                valid       ? "V" : "-",
                dirty       ? "D" : "-",
                referenced  ? "R" : "-");
    }
}
