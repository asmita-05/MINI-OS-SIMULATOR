package os.scheduler;

import os.process.Process;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Shortest Job First (SJF) — Non-Preemptive.
 *
 * Algorithm:
 *   When the CPU becomes free, the scheduler selects the ready process with
 *   the smallest burst time.  If multiple processes have equal burst time,
 *   ties are broken by arrival time, then PID.  A running process is never
 *   preempted; it executes until completion.
 *
 * Characteristics:
 *   - Optimal average waiting time among non-preemptive algorithms for a
 *     given workload.
 *   - Can starve long processes if short ones keep arriving.
 *   - Requires advance knowledge of burst times (impractical in real OSes,
 *     but valuable for theoretical analysis).
 */
public class SJFScheduler extends AbstractScheduler {

    @Override
    public String getName() { return "Shortest Job First - Non-Preemptive (SJF)"; }

    @Override
    public ScheduleResult simulate(List<Process> processes) {
        validate(processes);

        List<Process> procs = copyProcesses(processes);
        procs.sort(Comparator.comparingInt(Process::getArrivalTime)
                             .thenComparingInt(Process::getPid));

        // Ready queue ordered by burst time (then arrival, then PID)
        PriorityQueue<Process> readyQueue = new PriorityQueue<>(
                Comparator.comparingInt(Process::getBurstTime)
                          .thenComparingInt(Process::getArrivalTime)
                          .thenComparingInt(Process::getPid));

        List<GanttEntry> gantt     = new ArrayList<>();
        List<Process>    completed = new ArrayList<>();

        int time   = 0;
        int index  = 0;     // pointer into sorted procs (next process to arrive)
        int total  = procs.size();

        while (completed.size() < total) {

            // Enqueue all processes that have arrived by 'time'
            while (index < total && procs.get(index).getArrivalTime() <= time) {
                readyQueue.offer(procs.get(index++));
            }

            if (readyQueue.isEmpty()) {
                // CPU is idle — jump to the next arrival
                if (index < total) {
                    int idleEnd = procs.get(index).getArrivalTime();
                    gantt.add(GanttEntry.idle(time, idleEnd));
                    time = idleEnd;
                }
                continue;
            }

            Process p = readyQueue.poll();
            p.recordFirstScheduled(time);

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
