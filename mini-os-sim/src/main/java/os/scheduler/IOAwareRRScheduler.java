package os.scheduler;

import os.process.IOEvent;
import os.process.Process;
import os.process.ProcessState;

import java.util.*;

/**
 * I/O-Aware Round Robin Scheduler.
 *
 * Extends the standard Round Robin algorithm to model I/O bursts.
 * Each process may carry a list of {@link IOEvent}s that fire when a
 * specified amount of CPU time has been consumed.  When an event fires:
 *
 *   1. The process is moved to WAITING state and placed in an I/O queue.
 *   2. Time advances; processes in the I/O queue complete their I/O.
 *   3. Once I/O is done, the process re-enters the READY queue.
 *
 * This makes the simulation more realistic for processes that alternate
 * between CPU and I/O (the typical interactive workload model).
 *
 * Note: IOEvents are stored in a parallel map (pid → events) since the
 * base Process class is kept I/O-agnostic to preserve separation of concerns.
 */
public class IOAwareRRScheduler extends AbstractScheduler {

    private final int quantum;

    /** PID → ordered list of I/O events for that process */
    private Map<Integer, List<IOEvent>> ioEventMap;

    public IOAwareRRScheduler(int quantum) {
        if (quantum < 1) throw new IllegalArgumentException("Quantum must be >= 1");
        this.quantum = quantum;
    }

    public void setIOEvents(Map<Integer, List<IOEvent>> ioEventMap) {
        this.ioEventMap = ioEventMap;
    }

    @Override
    public String getName() { return "I/O-Aware Round Robin (Q=" + quantum + ")"; }

    @Override
    public ScheduleResult simulate(List<Process> processes) {
        validate(processes);

        List<Process> procs = copyProcesses(processes);
        procs.sort(Comparator.comparingInt(Process::getArrivalTime)
                             .thenComparingInt(Process::getPid));

        // cpu-used tracker per pid (for I/O trigger logic)
        Map<Integer, Integer> cpuUsed = new HashMap<>();
        for (Process p : procs) cpuUsed.put(p.getPid(), 0);

        // I/O waiting queue: process → time it will wake up
        TreeMap<Integer, Queue<Process>> ioWakeup = new TreeMap<>(); // wakeTime → processes

        Queue<Process>   readyQueue = new LinkedList<>();
        List<GanttEntry> gantt      = new ArrayList<>();
        List<Process>    completed  = new ArrayList<>();

        int time  = 0;
        int index = 0;
        int total = procs.size();

        if (!procs.isEmpty()) {
            time = procs.get(0).getArrivalTime();
            admitArrivals(procs, index, time, readyQueue);
            index = advanceIndex(procs, index, time);
        }

        while (completed.size() < total) {

            // Wake up any I/O-blocked processes whose I/O has completed
            wakeUpProcesses(ioWakeup, time, readyQueue);

            // Admit newly arrived processes
            admitArrivals(procs, index, time, readyQueue);
            index = advanceIndex(procs, index, time);

            if (readyQueue.isEmpty()) {
                // Determine next event: next arrival or next I/O wakeup
                int nextEvent = Integer.MAX_VALUE;
                if (index < total)
                    nextEvent = Math.min(nextEvent, procs.get(index).getArrivalTime());
                if (!ioWakeup.isEmpty())
                    nextEvent = Math.min(nextEvent, ioWakeup.firstKey());
                if (nextEvent == Integer.MAX_VALUE) break;

                gantt.add(GanttEntry.idle(time, nextEvent));
                time = nextEvent;
                continue;
            }

            Process p = readyQueue.poll();
            p.recordFirstScheduled(time);

            // How much CPU can this process use in this slice?
            int available = Math.min(quantum, p.getRemainingTime());

            // Check if an I/O event fires before the quantum expires
            int ioEventCpu = getNextIOTrigger(p.getPid(), cpuUsed.get(p.getPid()));
            int runTime    = available;
            IOEvent firingEvent = null;

            if (ioEventCpu > 0 && ioEventCpu <= available) {
                runTime     = ioEventCpu;
                firingEvent = consumeNextIOEvent(p.getPid(), cpuUsed.get(p.getPid()));
            }

            int start = time;
            time += runTime;
            cpuUsed.merge(p.getPid(), runTime, Integer::sum);
            p.setRemainingTime(p.getRemainingTime() - runTime);
            gantt.add(new GanttEntry(p.getPid(), p.getName(), start, time));

            // Admit arrivals that occurred during this burst
            admitArrivals(procs, index, time, readyQueue);
            index = advanceIndex(procs, index, time);

            if (p.getRemainingTime() == 0) {
                p.setState(ProcessState.TERMINATED);
                p.setCompletionTime(time);
                completed.add(p);
            } else if (firingEvent != null) {
                // Process goes to I/O wait
                p.setState(ProcessState.WAITING);
                int wakeTime = time + firingEvent.getIoDuration();
                ioWakeup.computeIfAbsent(wakeTime, k -> new LinkedList<>()).offer(p);
            } else {
                // Quantum expired — preempt back to ready queue
                p.setState(ProcessState.READY);
                readyQueue.offer(p);
            }
        }

        return buildResult(gantt, completed);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void admitArrivals(List<Process> procs, int fromIndex, int time,
                                Queue<Process> readyQueue) {
        for (int i = fromIndex; i < procs.size()
                && procs.get(i).getArrivalTime() <= time; i++) {
            if (procs.get(i).getState() == ProcessState.NEW) {
                procs.get(i).setState(ProcessState.READY);
                readyQueue.offer(procs.get(i));
            }
        }
    }

    private int advanceIndex(List<Process> procs, int current, int time) {
        while (current < procs.size() && procs.get(current).getArrivalTime() <= time)
            current++;
        return current;
    }

    private void wakeUpProcesses(TreeMap<Integer, Queue<Process>> ioWakeup,
                                  int time, Queue<Process> readyQueue) {
        Iterator<Map.Entry<Integer, Queue<Process>>> it =
                ioWakeup.headMap(time + 1, true).entrySet().iterator();
        while (it.hasNext()) {
            Queue<Process> waking = it.next().getValue();
            while (!waking.isEmpty()) {
                Process p = waking.poll();
                p.setState(ProcessState.READY);
                readyQueue.offer(p);
            }
            it.remove();
        }
    }

    /** Returns how many MORE CPU units until the next unfired I/O event, or -1. */
    private int getNextIOTrigger(int pid, int cpuAlreadyUsed) {
        if (ioEventMap == null) return -1;
        List<IOEvent> events = ioEventMap.get(pid);
        if (events == null) return -1;
        for (IOEvent e : events) {
            if (!e.isFired() && e.getTriggerAfterCpu() > cpuAlreadyUsed)
                return e.getTriggerAfterCpu() - cpuAlreadyUsed;
        }
        return -1;
    }

    private IOEvent consumeNextIOEvent(int pid, int cpuAlreadyUsed) {
        if (ioEventMap == null) return null;
        List<IOEvent> events = ioEventMap.get(pid);
        if (events == null) return null;
        for (IOEvent e : events) {
            if (!e.isFired() && e.getTriggerAfterCpu() > cpuAlreadyUsed) {
                e.markFired();
                return e;
            }
        }
        return null;
    }
}
