package os.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contiguous memory allocation manager.
 *
 * Maintains an ordered list of {@link MemoryBlock} objects whose addresses
 * are contiguous and non-overlapping, spanning [0, totalMemory).
 *
 * Allocation strategies:
 *  - FIRST_FIT  : scan left-to-right, choose first block that fits
 *  - BEST_FIT   : choose the smallest free block that fits
 *  - WORST_FIT  : choose the largest free block (maximises leftover size)
 *
 * Deallocation performs immediate coalescing: adjacent free blocks are merged
 * after every free() call to combat external fragmentation.
 */
public class ContiguousMemoryManager implements MemoryManager {

    private final int                totalMemory;
    private final AllocationStrategy strategy;
    private final List<MemoryBlock>  blocks;

    public ContiguousMemoryManager(int totalMemory, AllocationStrategy strategy) {
        if (totalMemory < 1)
            throw new IllegalArgumentException("Memory size must be >= 1");
        this.totalMemory = totalMemory;
        this.strategy    = strategy;
        this.blocks      = new ArrayList<>();
        this.blocks.add(new MemoryBlock(0, totalMemory)); // one big free block
    }

    @Override
    public int allocate(int pid, String processName, int size) {
        if (size < 1 || size > getFreeMemory()) return -1;

        MemoryBlock chosen = selectBlock(size);
        if (chosen == null) return -1;

        int chosenStart = chosen.getStartAddress();
        int chosenSize  = chosen.getSize();

        if (chosenSize == size) {
            // Perfect fit — no splitting needed
            chosen.allocate(pid, processName);
        } else {
            // Split: create a new free block for the remainder
            int idx = blocks.indexOf(chosen);
            chosen.setSize(size);
            chosen.allocate(pid, processName);
            blocks.add(idx + 1,
                    new MemoryBlock(chosenStart + size, chosenSize - size));
        }
        return chosenStart;
    }

    @Override
    public boolean free(int pid) {
        boolean freed = false;
        for (MemoryBlock b : blocks) {
            if (b.isAllocated() && b.getPid() == pid) {
                b.free();
                freed = true;
            }
        }
        if (freed) coalesce();
        return freed;
    }

    @Override
    public List<MemoryBlock> getMemoryMap() {
        return Collections.unmodifiableList(blocks);
    }

    @Override
    public int getTotalMemory() { return totalMemory; }

    @Override
    public int getFreeMemory() {
        return blocks.stream()
                     .filter(b -> !b.isAllocated())
                     .mapToInt(MemoryBlock::getSize)
                     .sum();
    }

    @Override
    public int getUsedMemory() { return totalMemory - getFreeMemory(); }

    @Override
    public void reset() {
        blocks.clear();
        blocks.add(new MemoryBlock(0, totalMemory));
    }

    public AllocationStrategy getStrategy() { return strategy; }

    // ── Private helpers ──────────────────────────────────────────────────────

    private MemoryBlock selectBlock(int size) {
        return switch (strategy) {
            case FIRST_FIT -> firstFit(size);
            case BEST_FIT  -> bestFit(size);
            case WORST_FIT -> worstFit(size);
        };
    }

    private MemoryBlock firstFit(int size) {
        for (MemoryBlock b : blocks)
            if (!b.isAllocated() && b.getSize() >= size) return b;
        return null;
    }

    private MemoryBlock bestFit(int size) {
        MemoryBlock best = null;
        for (MemoryBlock b : blocks) {
            if (!b.isAllocated() && b.getSize() >= size) {
                if (best == null || b.getSize() < best.getSize()) best = b;
            }
        }
        return best;
    }

    private MemoryBlock worstFit(int size) {
        MemoryBlock worst = null;
        for (MemoryBlock b : blocks) {
            if (!b.isAllocated() && b.getSize() >= size) {
                if (worst == null || b.getSize() > worst.getSize()) worst = b;
            }
        }
        return worst;
    }

    /**
     * Merges adjacent free blocks.
     * Called after every deallocation to reduce external fragmentation.
     */
    private void coalesce() {
        int i = 0;
        while (i < blocks.size() - 1) {
            MemoryBlock current = blocks.get(i);
            MemoryBlock next    = blocks.get(i + 1);
            if (!current.isAllocated() && !next.isAllocated()) {
                current.setSize(current.getSize() + next.getSize());
                blocks.remove(i + 1);
            } else {
                i++;
            }
        }
    }
}
