package os.scheduler;

import os.process.Process;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the correctness of all scheduling algorithms.
 *
 * Test strategy:
 *   1. Use small, hand-traceable process sets.
 *   2. Verify metric formulas (WT = TAT - Burst, etc.) hold for every process.
 *   3. Verify Gantt chart properties: no time-overlap, full coverage of busy time,
 *      completion times match process records.
 *   4. Edge cases: single process, all same arrival, burst=1, very large quantum.
 */
class SchedulerTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Builds processes from [pid, arrival, burst, priority] tuples. */
    private static List<Process> procs(int[][] data) {
        List<Process> list = new ArrayList<>();
        for (int[] d : data)
            list.add(new Process(d[0], "P" + d[0], d[1], d[2], d[3]));
        return list;
    }

    /**
     * Asserts that for every completed process:
     *   TAT = completion - arrival
     *   WT  = TAT - burst
     *   RT  >= 0
     *   CT  >= arrival + burst
     */
    private static void assertMetricConsistency(ScheduleResult result) {
        for (Process p : result.getCompletedProcesses()) {
            int expectedTAT = p.getCompletionTime() - p.getArrivalTime();
            assertEquals(expectedTAT, p.getTurnaroundTime(),
                    "TAT mismatch for " + p.getName());
            assertEquals(expectedTAT - p.getBurstTime(), p.getWaitingTime(),
                    "WT mismatch for " + p.getName());
            assertTrue(p.getResponseTime() >= 0,
                    "RT negative for " + p.getName());
            assertTrue(p.getCompletionTime() >= p.getArrivalTime() + p.getBurstTime(),
                    "CT too early for " + p.getName());
        }
    }

    /**
     * Asserts that Gantt chart entries are non-overlapping, ordered, and
     * cover all burst time accounted for in completed processes.
     */
    private static void assertGanttConsistency(ScheduleResult result) {
        List<GanttEntry> gantt = result.getGanttChart();
        assertFalse(gantt.isEmpty(), "Gantt chart should not be empty");

        for (int i = 1; i < gantt.size(); i++) {
            assertEquals(gantt.get(i - 1).getEndTime(), gantt.get(i).getStartTime(),
                    "Gantt gap between entries " + (i - 1) + " and " + i);
        }

        // Total busy (non-idle) duration must equal sum of all burst times
        int totalBurst = result.getCompletedProcesses().stream()
                .mapToInt(Process::getBurstTime).sum();
        int busyTime = gantt.stream().filter(e -> !e.isIdle())
                .mapToInt(GanttEntry::getDuration).sum();
        assertEquals(totalBurst, busyTime,
                "Gantt busy time != sum of burst times");
    }

    /**
     * Asserts all input processes appear in the completed list exactly once.
     */
    private static void assertAllCompleted(List<Process> input, ScheduleResult result) {
        assertEquals(input.size(), result.getCompletedProcesses().size(),
                "Not all processes were completed");
        Set<Integer> completedPids = new HashSet<>();
        for (Process p : result.getCompletedProcesses()) completedPids.add(p.getPid());
        for (Process p : input)
            assertTrue(completedPids.contains(p.getPid()),
                    "Process P" + p.getPid() + " missing from completed list");
    }

    // ── Classic textbook workload ────────────────────────────────────────────
    // P1(arr=0, burst=10), P2(arr=1, burst=4), P3(arr=2, burst=6)
    private static final int[][] CLASSIC = {{1, 0, 10, 3}, {2, 1, 4, 1}, {3, 2, 6, 4}};

    // ── FCFS ─────────────────────────────────────────────────────────────────

    @Test
    void fcfs_correctCompletionTimes() {
        List<Process> input = procs(CLASSIC);
        ScheduleResult r = new FCFSScheduler().simulate(input);

        assertAllCompleted(input, r);
        assertMetricConsistency(r);
        assertGanttConsistency(r);

        // Textbook expected: P1 finishes at 10, P2 at 14, P3 at 20
        Map<Integer, Integer> ct = completionMap(r);
        assertEquals(10, ct.get(1));
        assertEquals(14, ct.get(2));
        assertEquals(20, ct.get(3));
    }

    @Test
    void fcfs_averageWaitingTime() {
        List<Process> input = procs(CLASSIC);
        ScheduleResult r = new FCFSScheduler().simulate(input);
        // WT: P1=0, P2=9, P3=12  → avg = 7.0
        assertEquals(7.0, r.getAvgWaitingTime(), 0.001);
    }

    @Test
    void fcfs_idleGapWhenNoProcessAvailable() {
        // P1 arrives at t=5; CPU should be idle 0–5
        List<Process> input = List.of(new Process(1, "P1", 5, 3, 0));
        ScheduleResult r = new FCFSScheduler().simulate(input);
        assertTrue(r.getGanttChart().stream().anyMatch(GanttEntry::isIdle));
        assertEquals(5, r.getGanttChart().get(0).getEndTime());
    }

    // ── SJF ──────────────────────────────────────────────────────────────────

    @Test
    void sjf_selectsShortestJob() {
        List<Process> input = procs(CLASSIC);
        ScheduleResult r = new SJFScheduler().simulate(input);

        assertAllCompleted(input, r);
        assertMetricConsistency(r);
        assertGanttConsistency(r);

        // At t=10 (after P1), both P2(burst=4) and P3(burst=6) are in ready queue.
        // SJF picks P2 first.
        List<GanttEntry> gantt = r.getGanttChart();
        GanttEntry second = gantt.get(1);
        assertEquals(2, second.getPid(), "SJF should pick P2 (burst=4) after P1");
    }

    @Test
    void sjf_lowerAvgWaitThanFcfs() {
        List<Process> input = procs(CLASSIC);
        double fcfsWT = new FCFSScheduler().simulate(input).getAvgWaitingTime();
        double sjfWT  = new SJFScheduler().simulate(input).getAvgWaitingTime();
        assertTrue(sjfWT <= fcfsWT,
                "SJF avg WT should be <= FCFS avg WT");
    }

    // ── SRTF ─────────────────────────────────────────────────────────────────

    @Test
    void srtf_preemptsForShorterArrival() {
        // P1(arr=0, burst=8), P2(arr=1, burst=4)
        // At t=1, P2 arrives with remaining 4 < P1 remaining 7 → P2 preempts P1
        List<Process> input = List.of(
                new Process(1, "P1", 0, 8, 0),
                new Process(2, "P2", 1, 4, 0));
        ScheduleResult r = new SRTFScheduler().simulate(input);

        assertAllCompleted(input, r);
        assertMetricConsistency(r);
        assertGanttConsistency(r);

        // P2 should complete before P1
        Map<Integer, Integer> ct = completionMap(r);
        assertTrue(ct.get(2) < ct.get(1), "SRTF: P2 should complete before P1");
    }

    @Test
    void srtf_metricsConsistentOnClassic() {
        List<Process> input = procs(CLASSIC);
        ScheduleResult r = new SRTFScheduler().simulate(input);
        assertAllCompleted(input, r);
        assertMetricConsistency(r);
        assertGanttConsistency(r);
    }

    @Test
    void srtf_avgWtLeOrEqSjf() {
        List<Process> input = procs(CLASSIC);
        double sjfWT  = new SJFScheduler().simulate(input).getAvgWaitingTime();
        double srtfWT = new SRTFScheduler().simulate(input).getAvgWaitingTime();
        assertTrue(srtfWT <= sjfWT + 0.001,
                "SRTF avg WT should be <= SJF avg WT (preemption advantage)");
    }

    // ── Round Robin ───────────────────────────────────────────────────────────

    @Test
    void rr_allProcessesComplete() {
        List<Process> input = procs(CLASSIC);
        ScheduleResult r = new RoundRobinScheduler(3).simulate(input);
        assertAllCompleted(input, r);
        assertMetricConsistency(r);
        assertGanttConsistency(r);
    }

    @Test
    void rr_quantumLargerThanBurst_behavesLikeFcfs() {
        List<Process> input = procs(CLASSIC);
        ScheduleResult fcfs = new FCFSScheduler().simulate(input);
        ScheduleResult rr   = new RoundRobinScheduler(100).simulate(input);
        // When quantum >> max burst, every process runs to completion in one slice
        assertEquals(fcfs.getAvgWaitingTime(), rr.getAvgWaitingTime(), 0.001);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 5, 10})
    void rr_variousQuantums_allComplete(int q) {
        List<Process> input = procs(CLASSIC);
        ScheduleResult r = new RoundRobinScheduler(q).simulate(input);
        assertAllCompleted(input, r);
        assertGanttConsistency(r);
        assertMetricConsistency(r);
    }

    @Test
    void rr_invalidQuantumThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoundRobinScheduler(0));
    }

    // ── Priority (non-preemptive) ─────────────────────────────────────────────

    @Test
    void priority_np_selectsHighestPriorityFirst() {
        // All arrive at t=0; priority 0=highest
        List<Process> input = List.of(
                new Process(1, "P1", 0, 4, 3),
                new Process(2, "P2", 0, 3, 1),
                new Process(3, "P3", 0, 5, 2));
        ScheduleResult r = new PriorityScheduler(false).simulate(input);

        assertAllCompleted(input, r);
        assertMetricConsistency(r);
        assertGanttConsistency(r);

        // P2 (priority 1) should run first
        assertEquals(2, r.getGanttChart().get(0).getPid());
    }

    @Test
    void priority_p_preemptsOnHigherPriority() {
        // P1(arr=0, burst=10, pri=3), P2(arr=2, burst=4, pri=1)
        // P2 arrives at t=2 with higher priority → preempts P1
        List<Process> input = List.of(
                new Process(1, "P1", 0, 10, 3),
                new Process(2, "P2", 2, 4,  1));
        ScheduleResult r = new PriorityScheduler(true).simulate(input);

        assertAllCompleted(input, r);
        assertMetricConsistency(r);
        assertGanttConsistency(r);

        Map<Integer, Integer> ct = completionMap(r);
        assertTrue(ct.get(2) < ct.get(1), "P2 (higher priority) should complete before P1");
    }

    // ── MLQ ──────────────────────────────────────────────────────────────────

    @Test
    void mlq_partitionsAndCompletesAllProcesses() {
        // Mix of System (pri 0), Interactive (pri 3), Batch (pri 7)
        List<Process> input = List.of(
                new Process(1, "Sys",  0, 3, 0),
                new Process(2, "Int",  0, 4, 3),
                new Process(3, "Bat",  0, 5, 7));
        ScheduleResult r = new MLQScheduler().simulate(input);

        assertAllCompleted(input, r);
        assertMetricConsistency(r);

        // System process must complete before Interactive
        Map<Integer, Integer> ct = completionMap(r);
        assertTrue(ct.get(1) <= ct.get(2),
                "System process should not run after Interactive");
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void singleProcess_allAlgorithms() {
        List<Process> input = List.of(new Process(1, "Solo", 3, 7, 0));
        String[] algos = {"fcfs", "sjf", "srtf", "priority", "priority-p"};
        for (String algo : algos) {
            Scheduler s = SchedulerFactory.create(algo);
            ScheduleResult r = s.simulate(input);
            assertEquals(1, r.getCompletedProcesses().size(), algo + ": wrong completed count");
            assertEquals(10, r.getCompletedProcesses().get(0).getCompletionTime(),
                    algo + ": wrong CT for single process");
            assertEquals(0, r.getCompletedProcesses().get(0).getWaitingTime(),
                    algo + ": single process WT should be 0");
            assertGanttConsistency(r);
        }
    }

    @Test
    void nullInput_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new FCFSScheduler().simulate(null));
    }

    @Test
    void emptyInput_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new FCFSScheduler().simulate(Collections.emptyList()));
    }

    @Test
    void cpuUtilization_between0and1() {
        List<Process> input = procs(CLASSIC);
        for (String algo : SchedulerFactory.availableKeys()) {
            ScheduleResult r = SchedulerFactory.create(algo, 3).simulate(input);
            assertTrue(r.getCpuUtilization() >= 0 && r.getCpuUtilization() <= 1.0,
                    algo + ": CPU utilization out of range");
        }
    }

    @Test
    void originalProcessesNotMutated() {
        List<Process> input = procs(CLASSIC);
        int[] originalBursts = input.stream().mapToInt(Process::getBurstTime).toArray();
        new SRTFScheduler().simulate(input);
        for (int i = 0; i < input.size(); i++)
            assertEquals(originalBursts[i], input.get(i).getBurstTime(),
                    "Scheduler mutated original process burst time");
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static Map<Integer, Integer> completionMap(ScheduleResult r) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Process p : r.getCompletedProcesses()) map.put(p.getPid(), p.getCompletionTime());
        return map;
    }
}
