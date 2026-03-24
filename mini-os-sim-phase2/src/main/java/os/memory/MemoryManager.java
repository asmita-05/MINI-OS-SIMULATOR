package os.memory;

import java.util.List;

/**
 * Abstraction for the memory management subsystem.
 *
 * Concrete implementations may provide contiguous allocation strategies
 * (first-fit, best-fit, worst-fit) or more advanced schemes such as paging
 * or segmentation.  The interface is intentionally minimal so that alternative
 * implementations can be plugged in without modifying client code.
 */
public interface MemoryManager {

    /**
     * Allocates {@code size} units of memory to process {@code pid}.
     *
     * @return the starting address of the allocated block, or -1 on failure
     */
    int allocate(int pid, String processName, int size);

    /**
     * Frees all memory blocks currently allocated to process {@code pid}.
     *
     * @return {@code true} if at least one block was freed
     */
    boolean free(int pid);

    /** Returns an immutable snapshot of all memory blocks (free + allocated). */
    List<MemoryBlock> getMemoryMap();

    /** Total memory capacity in units. */
    int getTotalMemory();

    /** Sum of sizes of all free blocks. */
    int getFreeMemory();

    /** Sum of sizes of all allocated blocks. */
    int getUsedMemory();

    /**
     * External fragmentation ratio: free memory / total memory.
     * Does not reflect whether the free memory is contiguous.
     */
    default double getFragmentationRatio() {
        return getTotalMemory() > 0 ? (double) getFreeMemory() / getTotalMemory() : 0;
    }

    /** Resets all allocations, returning memory to a single free block. */
    void reset();
}
