package os.scheduler;

import os.process.Process;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * First Come First Served (FCFS) — Non-preemptive.
 *
 * Algorithm:
 *   Processes are ordered by arrival time.  The CPU is granted to each
 *   process in arrival order and held until completion.  If the CPU becomes
 *   idle (no process has arrived yet), idle time is recorded.
 *
 * Characteristics:
 *   - Simple, fair in arrival-time ordering.
 *   - Suffers from the "convoy effect" — a long process delays all later ones.
 *   - Average waiting time can be high for heterogeneous burst times.
 */
public class FCFSScheduler extends AbstractScheduler {

    @Override
    public String getName() { return "First Come First Served (FCFS)"; }

    @Override
    public ScheduleResult simulate(List<Process> processes) {
        validate(processes);

        // Work on copies — never mutate the caller's process list
        List<Process> procs = copyProcesses(processes);
        procs.sort(Comparator.comparingInt(Process::getArrivalTime)
                             .thenComparingInt(Process::getPid));

        List<GanttEntry> gantt     = new ArrayList<>();
        List<Process>    completed = new ArrayList<>();
        int time = 0;

        for (Process p : procs) {

            // CPU is idle until this process arrives
            if (time < p.getArrivalTime()) {
                gantt.add(GanttEntry.idle(time, p.getArrivalTime()));
                time = p.getArrivalTime();
            }

            // Record first-scheduled time (= response time start)
            p.recordFirstScheduled(time);

            // Execute to completion (non-preemptive)
            int start = time;
            time += p.getBurstTime();

            gantt.add(new GanttEntry(p.getPid(), p.getName(), start, time));
            p.setRemainingTime(0);
            p.setCompletionTime(time);
            completed.add(p);
        }

        return buildResult(gantt, completed);
    }
}
