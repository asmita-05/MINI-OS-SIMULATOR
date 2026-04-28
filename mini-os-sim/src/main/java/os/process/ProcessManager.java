package os.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<Integer, ProcessRuntime> runtimes = new LinkedHashMap<>();
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
        createAndStartRuntime(p);
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
        terminateRuntime(pid);
        processes.remove(p);
        return true;
    }

    /** Transitions a process to READY state (admitted from NEW). */
    public void admit(Process p) {
        if (p.getState() == ProcessState.NEW) {
            p.setState(ProcessState.READY);
            p.touch();
        }
    }

    /** Transitions READY → RUNNING. */
    public void dispatch(Process p) {
        if (p.getState() == ProcessState.READY || p.getState() == ProcessState.WAITING) {
            p.setState(ProcessState.RUNNING);
            p.touch();
            runtimeFor(p.getPid()).ifPresent(ProcessRuntime::requestRun);
        }
    }

    /** Transitions RUNNING → WAITING (blocked on I/O). */
    public void block(Process p) {
        if (p.getState() == ProcessState.RUNNING) {
            p.setState(ProcessState.WAITING);
            p.touch();
            runtimeFor(p.getPid()).ifPresent(ProcessRuntime::block);
        }
    }

    /** Transitions WAITING → READY (I/O complete). */
    public void wakeUp(Process p) {
        if (p.getState() == ProcessState.WAITING) {
            p.setState(ProcessState.READY);
            p.touch();
            runtimeFor(p.getPid()).ifPresent(ProcessRuntime::wakeUp);
        }
    }

    /** Transitions RUNNING → TERMINATED. */
    public void terminate(Process p) {
        p.setState(ProcessState.TERMINATED);
        p.touch();
        terminateRuntime(p.getPid());
    }

    public boolean startProcess(int pid) {
        Optional<Process> opt = findByPid(pid);
        if (opt.isEmpty()) return false;
        Process p = opt.get();
        if (p.getState() == ProcessState.TERMINATED) return false;
        p.setState(ProcessState.RUNNING);
        p.touch();
        return runtimeFor(pid).map(runtime -> {
            runtime.requestRun();
            return true;
        }).orElse(false);
    }

    public boolean pauseProcess(int pid) {
        Optional<Process> opt = findByPid(pid);
        if (opt.isEmpty()) return false;
        Process p = opt.get();
        if (p.getState() != ProcessState.RUNNING) return false;
        p.setState(ProcessState.READY);
        p.touch();
        return runtimeFor(pid).map(runtime -> {
            runtime.pause();
            return true;
        }).orElse(false);
    }

    public boolean blockProcess(int pid) {
        Optional<Process> opt = findByPid(pid);
        if (opt.isEmpty()) return false;
        Process p = opt.get();
        if (p.getState() == ProcessState.TERMINATED) return false;
        p.setState(ProcessState.WAITING);
        p.touch();
        return runtimeFor(pid).map(runtime -> {
            runtime.block();
            return true;
        }).orElse(false);
    }

    public boolean wakeProcess(int pid) {
        Optional<Process> opt = findByPid(pid);
        if (opt.isEmpty()) return false;
        Process p = opt.get();
        if (p.getState() != ProcessState.WAITING) return false;
        p.setState(ProcessState.READY);
        p.touch();
        return runtimeFor(pid).map(runtime -> {
            runtime.wakeUp();
            return true;
        }).orElse(false);
    }

    public boolean terminateProcess(int pid) {
        Optional<Process> opt = findByPid(pid);
        if (opt.isEmpty()) return false;
        Process p = opt.get();
        p.setState(ProcessState.TERMINATED);
        p.touch();
        terminateRuntime(pid);
        return true;
    }

    public Optional<ProcessRuntime> runtimeFor(int pid) {
        return Optional.ofNullable(runtimes.get(pid));
    }

    public void prepareForLiveScheduling() {
        for (Process p : processes) {
            recreateRuntime(p);
            p.resetExecutionState();
        }
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
        List<Integer> ids = new ArrayList<>(runtimes.keySet());
        for (int pid : ids) terminateRuntime(pid);
        processes.clear();
        runtimes.clear();
        nextPid = 1;
    }

    public int getNextPid() { return nextPid; }

    private void terminateRuntime(int pid) {
        ProcessRuntime runtime = runtimes.remove(pid);
        if (runtime != null) runtime.terminate();
    }

    private void recreateRuntime(Process process) {
        terminateRuntime(process.getPid());
        createAndStartRuntime(process);
    }

    private void createAndStartRuntime(Process process) {
        ProcessRuntime runtime = new ProcessRuntime(process);
        runtimes.put(process.getPid(), runtime);
        runtime.start();
    }
}
