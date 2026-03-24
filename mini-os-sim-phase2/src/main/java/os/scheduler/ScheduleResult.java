package os.scheduler;

import os.process.Process;
import java.util.List;

/**
 * Immutable value object returned by every scheduler after simulation.
 *
 * Bundles the Gantt chart timeline with the completed process list
 * (which carries per-process timing metadata) and aggregate metrics.
 */
public class ScheduleResult {

    private final List<GanttEntry> ganttChart;
    private final List<Process>    completedProcesses;

    // ── Aggregate metrics ────────────────────────────────────────────────────
    private final double avgWaitingTime;
    private final double avgTurnaroundTime;
    private final double avgResponseTime;
    private final double cpuUtilization;   // 0.0 – 1.0
    private final int    totalTime;

    public ScheduleResult(List<GanttEntry> ganttChart,
                          List<Process>    completedProcesses,
                          double avgWaitingTime,
                          double avgTurnaroundTime,
                          double avgResponseTime,
                          double cpuUtilization,
                          int    totalTime) {

        this.ganttChart          = List.copyOf(ganttChart);
        this.completedProcesses  = List.copyOf(completedProcesses);
        this.avgWaitingTime      = avgWaitingTime;
        this.avgTurnaroundTime   = avgTurnaroundTime;
        this.avgResponseTime     = avgResponseTime;
        this.cpuUtilization      = cpuUtilization;
        this.totalTime           = totalTime;
    }

    public List<GanttEntry> getGanttChart()         { return ganttChart; }
    public List<Process>    getCompletedProcesses() { return completedProcesses; }
    public double getAvgWaitingTime()               { return avgWaitingTime; }
    public double getAvgTurnaroundTime()            { return avgTurnaroundTime; }
    public double getAvgResponseTime()              { return avgResponseTime; }
    public double getCpuUtilization()               { return cpuUtilization; }
    public int    getTotalTime()                    { return totalTime; }

    /** Throughput = number of processes / total time */
    public double getThroughput() {
        return totalTime > 0 ? (double) completedProcesses.size() / totalTime : 0;
    }
}
