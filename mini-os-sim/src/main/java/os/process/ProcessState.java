package os.process;

/**
 * Represents the lifecycle states of a process in the OS simulator.
 * Models the standard five-state process model used in operating systems theory.
 */
public enum ProcessState {
    NEW,        // Process has been created but not yet admitted to ready queue
    READY,      // Process is in memory and waiting for CPU time
    RUNNING,    // Process is currently being executed on the CPU
    WAITING,    // Process is waiting for I/O or an event (blocked)
    TERMINATED  // Process has finished execution
}
