package os.scheduler;

import os.process.Process;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Shortest Remaining Time First (SRTF) — Preemptive SJF.
 *
 * Algorithm:
 *   The CPU is always allocated to the process with the smallest remaining
 *   burst time.  When a new process arrives with a remaining time shorter
 *   than the currently running process, the running process is preempted
 *   and placed back in the ready queue.
 *
 * Characteristics:
 *   - Theoretically optimal average waiting time.
 *   - High context-switch overhead and starvation risk for long jobs.
 *   - The simulation advances time event-by-event (at each arrival and
 *     completion) rather than tick-by-tick to avoid O(n·T) complexity.
 */
public class SRTFScheduler extends AbstractScheduler {

    @Override
    public String getName() { return "Shortest Remaining Time First - Preemptive (SRTF)"; }

    @Override
    public ScheduleResult simulate(List<Process> processes) {
        validate(processes);

        List<Process> procs = copyProcesses(processes);
        procs.sort(Comparator.comparingInt(Process::getArrivalTime)
                             .thenComparingInt(Process::getPid));

        // Ready queue ordered by remaining time, tie-break: arrival, PID
        PriorityQueue<Process> readyQueue = new PriorityQueue<>(
                Comparator.comparingInt(Process::getRemainingTime)
                          .thenComparingInt(Process::getArrivalTime)
                          .thenComparingInt(Process::getPid));

        List<GanttEntry> gantt     = new ArrayList<>();
        List<Process>    completed = new ArrayList<>();

        int time  = 0;
        int index = 0;
        int total = procs.size();

        // Seed the initial time to the first arrival
        if (!procs.isEmpty()) time = procs.get(0).getArrivalTime();

        while (completed.size() < total) {

            // Admit all processes that have arrived
            while (index < total && procs.get(index).getArrivalTime() <= time) {
                readyQueue.offer(procs.get(index++));
            }

            if (readyQueue.isEmpty()) {
                int nextArrival = procs.get(index).getArrivalTime();
                appendGantt(gantt, GanttEntry.IDLE_PID, "IDLE", time, nextArrival);
                time = nextArrival;
                continue;
            }

            Process running = readyQueue.poll();
            running.recordFirstScheduled(time);

            // Determine how long this process can run before a preemption event
            int runUntil = time + running.getRemainingTime(); // optimistic: run to completion

            // Check if any future arrival would preempt sooner
            while (index < total) {
                Process next = procs.get(index);
                if (next.getArrivalTime() >= runUntil) break; // arrives after current finishes

                int arrivalTime = next.getArrivalTime();
                int remainingIfRun = running.getRemainingTime() - (arrivalTime - time);

                if (next.getBurstTime() < remainingIfRun) {
                    // Preemption: next process arrives and has shorter remaining time
                    runUntil = arrivalTime;
                    break;
                } else {
                    // Next process arrives but won't preempt — admit it and continue
                    index++;
                    readyQueue.offer(next);
                }
            }

            int duration = runUntil - time;
            appendGantt(gantt, running.getPid(), running.getName(), time, runUntil);
            running.setRemainingTime(running.getRemainingTime() - duration);
            time = runUntil;

            if (running.getRemainingTime() == 0) {
                running.setCompletionTime(time);
                completed.add(running);
            } else {
                readyQueue.offer(running); // put back preempted process
            }
        }

        return buildResult(gantt, completed);
    }
}
