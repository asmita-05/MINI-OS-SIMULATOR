package os.scheduler;

import os.process.Process;
import java.util.*;

/**
 * Round Robin (RR) — Preemptive with configurable time quantum.
 *
 * Algorithm:
 *   Each process receives a fixed CPU time slice (quantum).  When the quantum
 *   expires the running process is preempted and placed at the tail of the
 *   ready queue.  New arrivals during a quantum are added to the queue after
 *   the current burst completes so that in-flight work is not retroactively
 *   delayed.
 *
 * Characteristics:
 *   - Good response time for interactive systems.
 *   - Context-switch overhead increases as quantum decreases.
 *   - Performance degrades toward FCFS when quantum ≥ max burst time.
 *   - Starvation-free by design.
 */
public class RoundRobinScheduler extends AbstractScheduler {

    private final int quantum;

    public RoundRobinScheduler(int quantum) {
        if (quantum < 1) throw new IllegalArgumentException("Time quantum must be >= 1");
        this.quantum = quantum;
    }

    @Override
    public String getName() { return "Round Robin (Q=" + quantum + ")"; }

    public int getQuantum() { return quantum; }

    @Override
    public ScheduleResult simulate(List<Process> processes) {
        validate(processes);

        List<Process> procs = copyProcesses(processes);
        procs.sort(Comparator.comparingInt(Process::getArrivalTime)
                             .thenComparingInt(Process::getPid));

        Queue<Process>   readyQueue = new LinkedList<>();
        List<GanttEntry> gantt      = new ArrayList<>();
        List<Process>    completed  = new ArrayList<>();

        int time  = 0;
        int index = 0;      // next unscheduled process in arrival-sorted list
        int total = procs.size();

        // Bootstrap: start at first arrival
        if (!procs.isEmpty()) {
            time = procs.get(0).getArrivalTime();
            while (index < total && procs.get(index).getArrivalTime() <= time)
                readyQueue.offer(procs.get(index++));
        }

        while (completed.size() < total) {

            if (readyQueue.isEmpty()) {
                // Idle — jump forward to next arrival
                int nextTime = procs.get(index).getArrivalTime();
                gantt.add(GanttEntry.idle(time, nextTime));
                time = nextTime;
                while (index < total && procs.get(index).getArrivalTime() <= time)
                    readyQueue.offer(procs.get(index++));
                continue;
            }

            Process p = readyQueue.poll();
            p.recordFirstScheduled(time);

            // Execute for min(quantum, remaining time)
            int execTime = Math.min(quantum, p.getRemainingTime());
            int start    = time;
            time        += execTime;

            gantt.add(new GanttEntry(p.getPid(), p.getName(), start, time));
            p.setRemainingTime(p.getRemainingTime() - execTime);

            // Admit processes that arrived during this CPU burst
            while (index < total && procs.get(index).getArrivalTime() <= time)
                readyQueue.offer(procs.get(index++));

            if (p.getRemainingTime() == 0) {
                p.setCompletionTime(time);
                completed.add(p);
            } else {
                readyQueue.offer(p);   // re-queue at tail
            }
        }

        return buildResult(gantt, completed);
    }
}
