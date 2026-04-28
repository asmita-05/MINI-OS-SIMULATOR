package os.scheduler;

import os.process.Process;

import java.util.*;

/**
 * Multilevel Queue (MLQ) Scheduler.
 *
 * Partitions processes into N priority levels (queues).  Each queue is
 * served with its own internal algorithm (configurable per level).
 * Higher-priority queues are always served before lower ones (strict
 * preemption between levels).
 *
 * Queue assignment:
 *   process.priority / levelsCount determines the queue index.
 *   Priority 0 → queue 0 (highest); higher numbers → lower queues.
 *
 * Within each queue, the algorithm is Round Robin by default (configurable).
 *
 * Design:
 *   MLQScheduler delegates per-queue simulation to existing Scheduler
 *   implementations, collecting Gantt entries into a single timeline.
 */
public class MLQScheduler extends AbstractScheduler {

    /**
     * Describes one level in the multilevel queue.
     */
    public static class QueueLevel {
        public final String    name;
        public final Scheduler algorithm;
        public final int       priorityMin;  // inclusive
        public final int       priorityMax;  // inclusive

        public QueueLevel(String name, Scheduler algorithm, int priorityMin, int priorityMax) {
            this.name        = name;
            this.algorithm   = algorithm;
            this.priorityMin = priorityMin;
            this.priorityMax = priorityMax;
        }
    }

    private final List<QueueLevel> levels;

    /**
     * Convenience constructor: creates a 3-level queue.
     *   Level 0 (System)      : priorities 0-1   — FCFS
     *   Level 1 (Interactive) : priorities 2-4   — Round Robin Q=4
     *   Level 2 (Batch)       : priorities 5+    — FCFS
     */
    public MLQScheduler() {
        this.levels = new ArrayList<>();
        levels.add(new QueueLevel("System",      new FCFSScheduler(),         0, 1));
        levels.add(new QueueLevel("Interactive", new RoundRobinScheduler(4),  2, 4));
        levels.add(new QueueLevel("Batch",       new FCFSScheduler(),         5, Integer.MAX_VALUE));
    }

    /** Full constructor: provide your own level definitions. */
    public MLQScheduler(List<QueueLevel> levels) {
        if (levels == null || levels.isEmpty())
            throw new IllegalArgumentException("Must supply at least one queue level.");
        this.levels = new ArrayList<>(levels);
    }

    @Override
    public String getName() { return "Multilevel Queue (MLQ)"; }

    @Override
    public ScheduleResult simulate(List<Process> processes) {
        validate(processes);

        List<Process> procs = copyProcesses(processes);

        // Partition processes across levels by priority
        List<List<Process>> partitions = new ArrayList<>();
        for (QueueLevel lvl : levels) {
            List<Process> partition = new ArrayList<>();
            for (Process p : procs)
                if (p.getPriority() >= lvl.priorityMin && p.getPriority() <= lvl.priorityMax)
                    partition.add(p);
            partitions.add(partition);
        }

        // Run each non-empty level in order (strict priority)
        // Adjust arrival times so higher-level jobs run first:
        // processes in level k are only eligible once all level 0..k-1 processes complete.
        List<GanttEntry> gantt     = new ArrayList<>();
        List<Process>    completed = new ArrayList<>();
        int timeOffset = 0;

        for (int i = 0; i < levels.size(); i++) {
            List<Process> partition = partitions.get(i);
            if (partition.isEmpty()) continue;

            // Shift arrival times to be after the previous level's completion
            // (simplified: treat each level as independent, serial execution)
            final int offset = timeOffset;
            List<Process> shifted = new ArrayList<>();
            for (Process p : partition) {
                // Create a shifted copy where arrival is max(original, offset)
                int shiftedArrival = Math.max(p.getArrivalTime(), offset);
                Process sp = new Process(p.getPid(), p.getName(),
                        shiftedArrival, p.getBurstTime(), p.getPriority());
                shifted.add(sp);
            }

            ScheduleResult levelResult = levels.get(i).algorithm.simulate(shifted);

            // Merge Gantt entries
            gantt.addAll(levelResult.getGanttChart());
            completed.addAll(levelResult.getCompletedProcesses());

            if (!levelResult.getGanttChart().isEmpty())
                timeOffset = levelResult.getGanttChart()
                                        .get(levelResult.getGanttChart().size() - 1)
                                        .getEndTime();
        }

        return buildResult(gantt, completed);
    }

    public List<QueueLevel> getLevels() { return Collections.unmodifiableList(levels); }
}
