package os.process;

/**
 * Models a single I/O event in a process's execution profile.
 *
 * A process may declare one or more I/O bursts that occur after consuming
 * a certain amount of CPU time.  The scheduler moves the process to WAITING
 * when an I/O event fires, and back to READY after the I/O duration elapses.
 *
 * Fields:
 *   triggerAfterCpu  — CPU units consumed before this I/O event fires
 *   ioDuration       — how many time units the I/O takes
 *   fired            — whether this event has already been triggered
 */
public class IOEvent {

    private final int     triggerAfterCpu;   // fire when process has used this much CPU
    private final int     ioDuration;
    private       boolean fired;

    public IOEvent(int triggerAfterCpu, int ioDuration) {
        if (triggerAfterCpu < 0) throw new IllegalArgumentException("Trigger CPU >= 0");
        if (ioDuration      < 1) throw new IllegalArgumentException("I/O duration >= 1");
        this.triggerAfterCpu = triggerAfterCpu;
        this.ioDuration      = ioDuration;
        this.fired           = false;
    }

    public int     getTriggerAfterCpu() { return triggerAfterCpu; }
    public int     getIoDuration()      { return ioDuration; }
    public boolean isFired()            { return fired; }
    public void    markFired()          { this.fired = true; }

    @Override
    public String toString() {
        return String.format("IOEvent[triggerCpu=%d, ioDuration=%d, fired=%b]",
                triggerAfterCpu, ioDuration, fired);
    }
}
