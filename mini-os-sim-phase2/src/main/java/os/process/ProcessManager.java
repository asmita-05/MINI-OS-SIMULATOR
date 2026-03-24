package os.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Central registry for all processes in the simulator.
 *
 * Acts as the "Process Control Block (PCB) table" — every process created
 * during a session is registered here and assigned a unique PID via an
 * auto-incrementing counter.  The manager exposes querying, state-transition
 * helpers, and bulk operations used by the CLI shell.
 */
public class ProcessManager {

    private final List<Process> processes = new ArrayList<>();
    private int nextPid = 1;

    // ── Process lifecycle ────────────────────────────────────────────────────

    /**
     * Creates and registers a new process.
     *
     * @param name        human-readable label
     * @param arrivalTime time unit at which the process enters the system
     * @param burstTime   CPU time required (ms / units)
     * @param priority    scheduling priority (0 = highest)
     * @return the newly created Process
     */
    public Process createProcess(String name, int arrivalTime, int burstTime, int priority) {
        Process p = new Process(nextPid++, name, arrivalTime, burstTime, priority);
        p.setState(ProcessState.NEW);
        processes.add(p);
        return p;
    }

    /**
     * Removes a process by PID.  Refuses to remove a RUNNING process.
     */
    public boolean removeProcess(int pid) {
        Optional<Process> opt = findByPid(pid);
        if (opt.isEmpty()) return false;
        Process p = opt.get();
        if (p.getState() == ProcessState.RUNNING) {
            System.out.println("[WARN] Cannot remove a RUNNING process (PID " + pid + ").");
            return false;
        }
        processes.remove(p);
        return true;
    }

    /** Transitions a process to READY state (admitted from NEW). */
    public void admit(Process p) {
        if (p.getState() == ProcessState.NEW) p.setState(ProcessState.READY);
    }

    /** Transitions READY → RUNNING. */
    public void dispatch(Process p) {
        if (p.getState() == ProcessState.READY) p.setState(ProcessState.RUNNING);
    }

    /** Transitions RUNNING → WAITING (blocked on I/O). */
    public void block(Process p) {
        if (p.getState() == ProcessState.RUNNING) p.setState(ProcessState.WAITING);
    }

    /** Transitions WAITING → READY (I/O complete). */
    public void wakeUp(Process p) {
        if (p.getState() == ProcessState.WAITING) p.setState(ProcessState.READY);
    }

    /** Transitions RUNNING → TERMINATED. */
    public void terminate(Process p) {
        p.setState(ProcessState.TERMINATED);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public Optional<Process> findByPid(int pid) {
        return processes.stream().filter(p -> p.getPid() == pid).findFirst();
    }

    public List<Process> getAll() {
        return Collections.unmodifiableList(processes);
    }

    public List<Process> getByState(ProcessState state) {
        List<Process> result = new ArrayList<>();
        for (Process p : processes) if (p.getState() == state) result.add(p);
        return result;
    }

    public int size()     { return processes.size(); }
    public boolean isEmpty() { return processes.isEmpty(); }

    /** Resets the manager to its initial empty state. */
    public void clear() {
        processes.clear();
        nextPid = 1;
    }

    public int getNextPid() { return nextPid; }
}
