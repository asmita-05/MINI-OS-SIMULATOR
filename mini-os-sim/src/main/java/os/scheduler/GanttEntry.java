package os.scheduler;

/**
 * Represents a single contiguous CPU burst on the Gantt chart timeline.
 *
 * A PID of -1 signals an idle (no runnable process) interval.
 */
public class GanttEntry {

    public static final int IDLE_PID = -1;

    private final int pid;
    private final String processName;
    private final int startTime;
    private final int endTime;

    public GanttEntry(int pid, String processName, int startTime, int endTime) {
        this.pid         = pid;
        this.processName = processName;
        this.startTime   = startTime;
        this.endTime     = endTime;
    }

    /** Convenience factory for idle intervals. */
    public static GanttEntry idle(int startTime, int endTime) {
        return new GanttEntry(IDLE_PID, "IDLE", startTime, endTime);
    }

    public int    getPid()         { return pid; }
    public String getProcessName() { return processName; }
    public int    getStartTime()   { return startTime; }
    public int    getEndTime()     { return endTime; }
    public int    getDuration()    { return endTime - startTime; }
    public boolean isIdle()        { return pid == IDLE_PID; }

    @Override
    public String toString() {
        return String.format("[P%d %d-%d]", pid, startTime, endTime);
    }
}
