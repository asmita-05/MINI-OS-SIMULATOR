package os.scheduler;

import os.process.Process;
import java.util.List;

/**
 * Strategy interface for all CPU scheduling algorithms.
 *
 * Each implementation receives an unordered list of processes and must return
 * a {@link ScheduleResult} containing the full Gantt chart and per-process
 * timing metrics.  Implementations MUST operate on deep copies of the input
 * processes so that the original {@link os.process.ProcessManager} state is
 * never mutated.
 *
 * Adding a new algorithm requires only:
 *   1. Implementing this interface.
 *   2. Registering the implementation in {@link SchedulerFactory}.
 */
public interface Scheduler {

    /** Returns a human-readable algorithm name (e.g., "Round Robin (Q=4)"). */
    String getName();

    /**
     * Runs the scheduling simulation on the given process list.
     *
     * @param processes list of processes to schedule (will not be mutated)
     * @return a fully computed {@link ScheduleResult}
     * @throws IllegalArgumentException if the process list is null or empty
     */
    ScheduleResult simulate(List<Process> processes);
}
