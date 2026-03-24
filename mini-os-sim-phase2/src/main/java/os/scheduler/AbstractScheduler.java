package os.scheduler;

import os.process.Process;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class that provides common helper utilities shared by all
 * concrete scheduler implementations.
 *
 * Subclasses only need to implement {@code simulate()} — metric aggregation,
 * deep-copy utilities, and Gantt-entry compression are handled here.
 */
public abstract class AbstractScheduler implements Scheduler {

    // ── Shared helpers ───────────────────────────────────────────────────────

    /**
     * Deep-copies a list of processes so the simulation never mutates
     * the original PCB table held by ProcessManager.
     */
    protected List<Process> copyProcesses(List<Process> source) {
        List<Process> copies = new ArrayList<>(source.size());
        for (Process p : source) copies.add(p.copy());
        return copies;
    }

    /**
     * Computes a {@link ScheduleResult} from raw Gantt entries and
     * the list of completed (simulation-copy) processes.
     */
    protected ScheduleResult buildResult(List<GanttEntry> gantt,
                                         List<Process>    completed) {

        int n = completed.size();
        if (n == 0) return new ScheduleResult(gantt, completed, 0, 0, 0, 0, 0);

        double sumWT = 0, sumTAT = 0, sumRT = 0;
        for (Process p : completed) {
            sumWT  += p.getWaitingTime();
            sumTAT += p.getTurnaroundTime();
            sumRT  += p.getResponseTime();
        }

        int totalTime  = gantt.isEmpty() ? 0 : gantt.get(gantt.size() - 1).getEndTime();
        int busyTime   = gantt.stream().filter(e -> !e.isIdle()).mapToInt(GanttEntry::getDuration).sum();
        double utilization = totalTime > 0 ? (double) busyTime / totalTime : 0.0;

        return new ScheduleResult(
                gantt, completed,
                sumWT  / n,
                sumTAT / n,
                sumRT  / n,
                utilization,
                totalTime);
    }

    /**
     * Appends a Gantt entry, merging it with the last entry when the PID
     * is identical (avoids thousands of 1-unit entries for SRTF).
     */
    protected void appendGantt(List<GanttEntry> gantt, int pid, String name,
                                int start, int end) {
        if (!gantt.isEmpty()) {
            GanttEntry last = gantt.get(gantt.size() - 1);
            if (last.getPid() == pid && last.getEndTime() == start) {
                gantt.set(gantt.size() - 1,
                        new GanttEntry(pid, name, last.getStartTime(), end));
                return;
            }
        }
        gantt.add(new GanttEntry(pid, name, start, end));
    }

    /** Validates that the input list is non-null and non-empty. */
    protected void validate(List<Process> processes) {
        if (processes == null || processes.isEmpty())
            throw new IllegalArgumentException("Process list must not be null or empty.");
    }
}
