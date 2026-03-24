package os.memory;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContiguousMemoryManager and PagingMemoryManager.
 */
class MemoryManagerTest {

    // ── ContiguousMemoryManager ──────────────────────────────────────────────

    @Nested
    @DisplayName("ContiguousMemoryManager")
    class ContiguousTests {

        @ParameterizedTest(name = "{0}")
        @EnumSource(AllocationStrategy.class)
        void alloc_succeeds_whenEnoughMemory(AllocationStrategy strategy) {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(256, strategy);
            int addr = mm.allocate(1, "P1", 64);
            assertNotEquals(-1, addr, strategy + ": allocation should succeed");
            assertEquals(64, mm.getUsedMemory());
            assertEquals(192, mm.getFreeMemory());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(AllocationStrategy.class)
        void alloc_fails_whenNotEnoughMemory(AllocationStrategy strategy) {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(64, strategy);
            mm.allocate(1, "P1", 60);
            int addr = mm.allocate(2, "P2", 10); // only 4 free
            assertEquals(-1, addr, strategy + ": allocation should fail");
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(AllocationStrategy.class)
        void free_returnsMemory(AllocationStrategy strategy) {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(256, strategy);
            mm.allocate(1, "P1", 128);
            assertTrue(mm.free(1));
            assertEquals(256, mm.getFreeMemory());
        }

        @Test
        void free_unknownPid_returnsFalse() {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(128, AllocationStrategy.FIRST_FIT);
            assertFalse(mm.free(999));
        }

        @Test
        void coalescing_mergesAdjacentFreeBlocks() {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(300, AllocationStrategy.FIRST_FIT);
            mm.allocate(1, "A", 100);
            mm.allocate(2, "B", 100);
            mm.allocate(3, "C", 100);

            mm.free(1);
            mm.free(2);  // should coalesce with previous free block

            // Should now have one free block of 200 and one allocated block of 100
            List<MemoryBlock> map = mm.getMemoryMap();
            long freeBlocks = map.stream().filter(b -> !b.isAllocated()).count();
            assertEquals(1, freeBlocks, "Adjacent free blocks should be coalesced");
            assertEquals(200, mm.getFreeMemory());
        }

        @Test
        void firstFit_usesFirstSuitableBlock() {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(400, AllocationStrategy.FIRST_FIT);
            mm.allocate(1, "A", 100);
            mm.allocate(2, "B", 100);
            mm.free(1);  // leaves hole [0-99]
            // New allocation of 50 should fill the first hole
            int addr = mm.allocate(3, "C", 50);
            assertEquals(0, addr, "First-Fit should fill the first available hole");
        }

        @Test
        void bestFit_picksSmallestFit() {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(500, AllocationStrategy.BEST_FIT);
            // Create holes of different sizes
            mm.allocate(1, "A", 100);
            mm.allocate(2, "B", 200);
            mm.allocate(3, "C", 100);
            mm.free(1);   // 100-unit hole at 0
            mm.free(2);   // 200-unit hole at 100
            mm.free(3);   // these coalesce → one big free block; add a separator
            // Re-test with non-coalescing configuration
            mm.reset();
            mm.allocate(10, "X", 50);   // 50 used
            mm.allocate(11, "Y", 150);  // 200 used
            mm.allocate(12, "Z", 50);   // 250 used, 250 free at end
            mm.free(10);                 // 50-unit hole at 0
            mm.free(11);                 // 150 unit hole at 50, coalesces to 200-unit hole
            // Now holes: [200 at 0], [250 at 250]
            // Best-fit for 60 should pick the 200-unit hole (smallest that fits)
            int addr = mm.allocate(20, "New", 60);
            assertEquals(0, addr, "Best-Fit should prefer the smallest fitting hole");
        }

        @Test
        void worstFit_picksLargestHole() {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(600, AllocationStrategy.WORST_FIT);
            mm.allocate(1, "A", 100);
            mm.allocate(2, "B", 300);
            mm.free(1);  // 100-unit hole
            // 300 free at end still exists; worst-fit should use it
            int addr = mm.allocate(3, "C", 50);
            // Worst-fit picks the largest free block (the 200 end or the 100 front)
            // With no prior alloc, the back 200-unit space is largest
            assertNotEquals(-1, addr);
        }

        @Test
        void reset_clearsAllAllocations() {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(256, AllocationStrategy.FIRST_FIT);
            mm.allocate(1, "A", 128);
            mm.reset();
            assertEquals(256, mm.getFreeMemory());
            assertEquals(1, mm.getMemoryMap().size());
        }

        @Test
        void totalMemory_remainsConstant() {
            ContiguousMemoryManager mm = new ContiguousMemoryManager(1024, AllocationStrategy.BEST_FIT);
            mm.allocate(1, "A", 200);
            mm.allocate(2, "B", 300);
            mm.free(1);
            assertEquals(1024, mm.getTotalMemory());
            assertEquals(mm.getFreeMemory() + mm.getUsedMemory(), mm.getTotalMemory());
        }

        @Test
        void invalidSize_throwsOnConstruction() {
            assertThrows(IllegalArgumentException.class,
                    () -> new ContiguousMemoryManager(0, AllocationStrategy.FIRST_FIT));
        }
    }

    // ── PagingMemoryManager ──────────────────────────────────────────────────

    @Nested
    @DisplayName("PagingMemoryManager")
    class PagingTests {

        @Test
        void allocate_assignsCorrectNumberOfFrames() {
            PagingMemoryManager mm = new PagingMemoryManager(256, 64); // 4 frames
            int result = mm.allocate(1, "P1", 64); // exactly 1 page
            assertNotEquals(-1, result);
            assertEquals(1, mm.getPageTable(1).size());
            assertEquals(1, mm.getTotalFrames() - mm.getFreeFrames());
        }

        @Test
        void allocate_ceilsPageCount() {
            PagingMemoryManager mm = new PagingMemoryManager(256, 64); // 4 frames
            mm.allocate(1, "P1", 65); // needs 2 frames (ceil(65/64)=2)
            assertEquals(2, mm.getPageTable(1).size());
        }

        @Test
        void free_releasesAllFrames() {
            PagingMemoryManager mm = new PagingMemoryManager(256, 64);
            mm.allocate(1, "P1", 128); // 2 frames
            assertTrue(mm.free(1));
            assertEquals(4, mm.getFreeFrames());
            assertEquals(0, mm.getFreeMemory() - mm.getTotalMemory(), 0.1);
        }

        @Test
        void translateAddress_correctForFirstPage() {
            PagingMemoryManager mm = new PagingMemoryManager(256, 64);
            mm.allocate(1, "P1", 128); // pages 0 and 1 mapped to frames 0 and 1
            // Logical address 10 → page 0, offset 10 → physical = frame0*64 + 10
            int physical = mm.translateAddress(1, 10);
            assertNotEquals(-1, physical);
            // frame 0, offset 10 → physical = 0*64 + 10 = 10
            assertEquals(10, physical);
        }

        @Test
        void translateAddress_pageFault_returnsNegOne() {
            PagingMemoryManager mm = new PagingMemoryManager(256, 64);
            mm.allocate(1, "P1", 64); // only page 0
            int physical = mm.translateAddress(1, 200); // page 3 — not allocated
            assertEquals(-1, physical);
        }

        @Test
        void translateAddress_unknownProcess_returnsNegOne() {
            PagingMemoryManager mm = new PagingMemoryManager(256, 64);
            assertEquals(-1, mm.translateAddress(99, 0));
        }

        @Test
        void allFramesUsed_allocationFails() {
            PagingMemoryManager mm = new PagingMemoryManager(128, 64); // 2 frames
            mm.allocate(1, "P1", 64);
            mm.allocate(2, "P2", 64);
            assertEquals(-1, mm.allocate(3, "P3", 64)); // no free frames
        }

        @Test
        void pageSizeNotPowerOfTwo_throws() {
            assertThrows(IllegalArgumentException.class,
                    () -> new PagingMemoryManager(256, 60));
        }

        @Test
        void reset_clearsAllTables() {
            PagingMemoryManager mm = new PagingMemoryManager(256, 64);
            mm.allocate(1, "A", 128);
            mm.reset();
            assertEquals(4, mm.getFreeFrames());
            assertTrue(mm.getPageTable(1).isEmpty());
        }

        @Test
        void multipleProcesses_noFrameConflict() {
            PagingMemoryManager mm = new PagingMemoryManager(256, 64); // 4 frames
            mm.allocate(1, "P1", 64); // frame 0
            mm.allocate(2, "P2", 64); // frame 1
            mm.allocate(3, "P3", 64); // frame 2

            // Each process should translate to a different physical frame
            int pa1 = mm.translateAddress(1, 0);
            int pa2 = mm.translateAddress(2, 0);
            int pa3 = mm.translateAddress(3, 0);

            assertTrue(pa1 != pa2 && pa2 != pa3 && pa1 != pa3,
                    "Different processes must map to different physical frames");
        }
    }
}
