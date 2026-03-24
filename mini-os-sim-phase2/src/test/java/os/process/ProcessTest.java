package os.process;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Process model and ProcessManager.
 */
class ProcessTest {

    private ProcessManager pm;

    @BeforeEach
    void setUp() { pm = new ProcessManager(); }

    // ── Process creation ─────────────────────────────────────────────────────

    @Test
    void create_assignsIncrementingPids() {
        Process p1 = pm.createProcess("A", 0, 5, 0);
        Process p2 = pm.createProcess("B", 1, 3, 0);
        assertEquals(1, p1.getPid());
        assertEquals(2, p2.getPid());
    }

    @Test
    void create_initialStateIsNew() {
        Process p = pm.createProcess("X", 0, 4, 0);
        assertEquals(ProcessState.NEW, p.getState());
    }

    @Test
    void create_invalidBurstThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> pm.createProcess("Bad", 0, 0, 0));
    }

    @Test
    void create_negativeArrivalThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> pm.createProcess("Bad", -1, 5, 0));
    }

    // ── State transitions ────────────────────────────────────────────────────

    @Test
    void admit_transitionsNewToReady() {
        Process p = pm.createProcess("P", 0, 5, 0);
        pm.admit(p);
        assertEquals(ProcessState.READY, p.getState());
    }

    @Test
    void admit_ignoredIfNotNew() {
        Process p = pm.createProcess("P", 0, 5, 0);
        pm.admit(p);
        pm.admit(p);   // second admit should be no-op
        assertEquals(ProcessState.READY, p.getState());
    }

    @Test
    void dispatch_readyToRunning() {
        Process p = pm.createProcess("P", 0, 5, 0);
        pm.admit(p);
        pm.dispatch(p);
        assertEquals(ProcessState.RUNNING, p.getState());
    }

    @Test
    void block_runningToWaiting() {
        Process p = pm.createProcess("P", 0, 5, 0);
        pm.admit(p); pm.dispatch(p); pm.block(p);
        assertEquals(ProcessState.WAITING, p.getState());
    }

    @Test
    void wakeUp_waitingToReady() {
        Process p = pm.createProcess("P", 0, 5, 0);
        pm.admit(p); pm.dispatch(p); pm.block(p); pm.wakeUp(p);
        assertEquals(ProcessState.READY, p.getState());
    }

    @Test
    void terminate_setsTerminated() {
        Process p = pm.createProcess("P", 0, 5, 0);
        pm.admit(p); pm.dispatch(p); pm.terminate(p);
        assertEquals(ProcessState.TERMINATED, p.getState());
    }

    // ── Derived metrics ──────────────────────────────────────────────────────

    @Test
    void metrics_correctAfterManualSet() {
        Process p = pm.createProcess("P", 2, 5, 0);
        p.recordFirstScheduled(4);
        p.setCompletionTime(12);

        assertEquals(10, p.getTurnaroundTime()); // 12 - 2
        assertEquals(5,  p.getWaitingTime());    // 10 - 5
        assertEquals(2,  p.getResponseTime());   // 4  - 2
    }

    // ── Manager queries ──────────────────────────────────────────────────────

    @Test
    void findByPid_returnsCorrectProcess() {
        Process p = pm.createProcess("Z", 0, 3, 1);
        assertTrue(pm.findByPid(p.getPid()).isPresent());
        assertEquals("Z", pm.findByPid(p.getPid()).get().getName());
    }

    @Test
    void findByPid_emptyForMissingPid() {
        assertTrue(pm.findByPid(999).isEmpty());
    }

    @Test
    void remove_decreasesCount() {
        Process p = pm.createProcess("R", 0, 4, 0);
        pm.admit(p);
        assertTrue(pm.removeProcess(p.getPid()));
        assertEquals(0, pm.size());
    }

    @Test
    void remove_runningProcessFails() {
        Process p = pm.createProcess("R", 0, 4, 0);
        pm.admit(p); pm.dispatch(p);
        assertFalse(pm.removeProcess(p.getPid()));
        assertEquals(1, pm.size());
    }

    @Test
    void clear_removesAllProcesses() {
        pm.createProcess("A", 0, 2, 0);
        pm.createProcess("B", 1, 3, 0);
        pm.clear();
        assertEquals(0, pm.size());
        assertEquals(1, pm.getNextPid()); // counter resets
    }

    // ── Deep copy ────────────────────────────────────────────────────────────

    @Test
    void copy_producesIndependentProcess() {
        Process original = pm.createProcess("Orig", 0, 8, 2);
        pm.admit(original);
        Process copy = original.copy();

        copy.setRemainingTime(3);
        assertEquals(8, original.getRemainingTime());
        assertEquals(3, copy.getRemainingTime());
        assertEquals(ProcessState.NEW, copy.getState());
    }
}
