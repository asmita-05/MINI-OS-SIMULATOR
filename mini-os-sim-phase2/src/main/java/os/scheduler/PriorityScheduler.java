package os.scheduler;

import os.process.Process;
import java.util.*;

/**
 * Priority Scheduling.
 *
 * Two modes are supported via constructor flag:
 *
 * Non-Preemptive:
 *   When the CPU becomes free the ready process with the highest priority
 *   (numerically smallest value) is selected and runs to completion.
 *
 * Preemptive:
 *   When a new process arrives with higher priority than the running process,
 *   the running process is preempted.  The simulation is event-driven:
 *   time jumps to the next arrival or completion rather than advancing
 *   one tick at a time.
 *
 * Tie-breaking:
 *   Equal priority → earlier arrival; equal arrival → lower PID.
 *
 * Starvation note:
 *   In a long-running workload, low-priority processes may starve.
 *   Real OSes address this with aging (not modelled here but the design
 *   supports it as a future extension).
 */
public class PriorityScheduler extends AbstractScheduler {

    private final boolean preemptive;

    private static final Comparator<Process> PRIORITY_ORDER =
            Comparator.comparingInt(Process::getPriority)
                      .thenComparingInt(Process::getArrivalTime)
                      .thenComparingInt(Process::getPid);

    public PriorityScheduler(boolean preemptive) {
        this.preemptive = preemptive;
    }

    @Override
    public String getName() {
        return preemptive ? "Priority Scheduling - Preemptive"
                          : "Priority Scheduling - Non-Preemptive";
    }

    public boolean isPreemptive() { return preemptive; }

    @Override
    public ScheduleResult simulate(List<Process> processes) {
        validate(processes);

        List<Process> procs = copyProcesses(processes);
        procs.sort(Comparator.comparingInt(Process::getArrivalTime)
                             .thenComparingInt(Process::getPid));

        PriorityQueue<Process> readyQueue = new PriorityQueue<>(PRIORITY_ORDER);
        List<GanttEntry> gantt     = new ArrayList<>();
        List<Process>    completed = new ArrayList<>();

        int time  = 0;
        int index = 0;
        int total = procs.size();

        if (!procs.isEmpty()) time = procs.get(0).getArrivalTime();

        while (completed.size() < total) {

            while (index < total && procs.get(index).getArrivalTime() <= time)
                readyQueue.offer(procs.get(index++));

            if (readyQueue.isEmpty()) {
                int nextArrival = procs.get(index).getArrivalTime();
                appendGantt(gantt, GanttEntry.IDLE_PID, "IDLE", time, nextArrival);
                time = nextArrival;
                continue;
            }

            if (!preemptive) {
                // ── Non-preemptive: run to completion ────────────────────────
                Process p = readyQueue.poll();
                p.recordFirstScheduled(time);
                int start = time;
                time += p.getBurstTime();
                gantt.add(new GanttEntry(p.getPid(), p.getName(), start, time));
                p.setRemainingTime(0);
                p.setCompletionTime(time);
                completed.add(p);

            } else {
                // ── Preemptive: run until preemption or completion ───────────
                Process running = readyQueue.poll();
                running.recordFirstScheduled(time);

                // Find the earliest future event that could cause preemption
                int runUntil = time + running.getRemainingTime();

                for (int i = index; i < total; i++) {
                    int arrTime = procs.get(i).getArrivalTime();
                    if (arrTime >= runUntil) break;

                    // Would the arriving process preempt the running one?
                    int remainingAtArrival = running.getRemainingTime() - (arrTime - time);
                    if (PRIORITY_ORDER.compare(procs.get(i), running) < 0
                            && remainingAtArrival > 0) {
                        runUntil = arrTime;
                        break;
                    } else {
                        // Admit non-preempting arrivals immediately
                        readyQueue.offer(procs.get(i));
                        index++;
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
                    readyQueue.offer(running);
                }
            }
        }

        return buildResult(gantt, completed);
    }
}
