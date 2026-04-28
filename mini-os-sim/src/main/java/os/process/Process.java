package os.process;

/**
 * Models a process as a first-class entity in the OS simulator.
 *
 * Encapsulates both static attributes (PID, burst time, arrival time, priority)
 * and dynamic simulation state (remaining time, completion time, context metadata).
 * Process objects are designed to be cloneable so schedulers can operate on
 * isolated copies without mutating the original process list.
 */
public class Process {

    // ── Static attributes ────────────────────────────────────────────────────
    private final int    pid;
    private final String name;
    private final int    arrivalTime;
    private final int    burstTime;
    private final int    priority;       // lower number = higher priority

    // ── Dynamic / simulation state ───────────────────────────────────────────
    private ProcessState state;
    private int          remainingTime;

    // ── Completion & timing metadata (set by scheduler) ──────────────────────
    private int  completionTime;
    private int  firstScheduledTime;     // used to compute response time
    private boolean firstScheduled;

    // ── Memory mapping (set by MemoryManager) ────────────────────────────────
    private int  memoryStart   = -1;
    private int  memorySize    =  0;
    
    // Live runtime metadata for thread-backed process management
    private boolean threadBacked;
    private long    javaThreadId      = -1;
    private String  javaThreadName    = "";
    private String  javaThreadState   = "NEW";
    private long    runtimeTicks      = 0;
    private long    messagesSent      = 0;
    private long    messagesReceived  = 0;
    private long    createdAtMillis   = System.currentTimeMillis();
    private long    lastActiveAtMillis = createdAtMillis;

    // ─────────────────────────────────────────────────────────────────────────

    public Process(int pid, String name, int arrivalTime, int burstTime, int priority) {
        if (arrivalTime < 0) throw new IllegalArgumentException("Arrival time must be >= 0");
        if (burstTime   < 1) throw new IllegalArgumentException("Burst time must be >= 1");
        if (priority    < 0) throw new IllegalArgumentException("Priority must be >= 0");

        this.pid           = pid;
        this.name          = name;
        this.arrivalTime   = arrivalTime;
        this.burstTime     = burstTime;
        this.priority      = priority;
        this.remainingTime = burstTime;
        this.state         = ProcessState.NEW;
        this.firstScheduled = false;
    }

    /**
     * Returns a deep copy of this process with simulation fields reset to
     * initial values. Schedulers should always operate on copies.
     */
    public Process copy() {
        Process p = new Process(pid, name, arrivalTime, burstTime, priority);
        p.state          = ProcessState.NEW;
        p.remainingTime  = burstTime;
        p.firstScheduled = false;
        return p;
    }

    // ── Derived performance metrics ──────────────────────────────────────────

    /** Turnaround Time = Completion Time − Arrival Time */
    public int getTurnaroundTime() {
        return completionTime - arrivalTime;
    }

    /** Waiting Time = Turnaround Time − Burst Time */
    public int getWaitingTime() {
        return getTurnaroundTime() - burstTime;
    }

    /** Response Time = First CPU Time − Arrival Time */
    public int getResponseTime() {
        return firstScheduledTime - arrivalTime;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int          getPid()             { return pid; }
    public String       getName()            { return name; }
    public int          getArrivalTime()     { return arrivalTime; }
    public int          getBurstTime()       { return burstTime; }
    public int          getPriority()        { return priority; }
    public ProcessState getState()           { return state; }
    public int          getRemainingTime()   { return remainingTime; }
    public int          getCompletionTime()  { return completionTime; }
    public int          getMemoryStart()     { return memoryStart; }
    public int          getMemorySize()      { return memorySize; }
    public boolean      isFirstScheduled()   { return firstScheduled; }
    public int          getFirstScheduledTime() { return firstScheduledTime; }
    public boolean      isThreadBacked()     { return threadBacked; }
    public long         getJavaThreadId()    { return javaThreadId; }
    public String       getJavaThreadName()  { return javaThreadName; }
    public String       getJavaThreadState() { return javaThreadState; }
    public long         getRuntimeTicks()    { return runtimeTicks; }
    public long         getMessagesSent()    { return messagesSent; }
    public long         getMessagesReceived(){ return messagesReceived; }
    public long         getCreatedAtMillis() { return createdAtMillis; }
    public long         getLastActiveAtMillis() { return lastActiveAtMillis; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setState(ProcessState s)     { this.state = s; }
    public void setRemainingTime(int t)      { this.remainingTime = t; }
    public void setCompletionTime(int t)     { this.completionTime = t; }
    public void setMemoryStart(int s)        { this.memoryStart = s; }
    public void setMemorySize(int s)         { this.memorySize = s; }
    public void setThreadBacked(boolean v)   { this.threadBacked = v; }
    public void setJavaThreadState(String s) { this.javaThreadState = s; }
    public void setCreatedAtMillis(long t)   { this.createdAtMillis = t; }
    public void setLastActiveAtMillis(long t){ this.lastActiveAtMillis = t; }

    /**
     * Records the first time this process was given the CPU.
     * Subsequent calls are no-ops (idempotent).
     */
    public void recordFirstScheduled(int time) {
        if (!firstScheduled) {
            this.firstScheduledTime = time;
            this.firstScheduled     = true;
        }
    }

    public void bindJavaThread(Thread worker) {
        if (worker == null) return;
        this.threadBacked    = true;
        this.javaThreadId    = worker.getId();
        this.javaThreadName  = worker.getName();
        this.javaThreadState = worker.getState().name();
    }

    public void incrementRuntimeTicks() {
        runtimeTicks++;
        touch();
    }

    public void incrementMessagesSent() {
        messagesSent++;
        touch();
    }

    public void incrementMessagesReceived() {
        messagesReceived++;
        touch();
    }

    public void touch() {
        lastActiveAtMillis = System.currentTimeMillis();
    }

    public void resetExecutionState() {
        this.state              = ProcessState.NEW;
        this.remainingTime      = burstTime;
        this.completionTime     = 0;
        this.firstScheduledTime = 0;
        this.firstScheduled     = false;
        this.javaThreadState    = "NEW";
        this.runtimeTicks       = 0;
        this.touch();
    }

    @Override
    public String toString() {
        return String.format("P%d[%s | arr=%d bst=%d pri=%d]",
                pid, name, arrivalTime, burstTime, priority);
    }
}
