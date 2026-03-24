package os.scheduler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Factory / registry for scheduling algorithms.
 * Phase 2: FCFS, SJF, Round Robin, Priority (non-preemptive) implemented.
 * Phase 3: SRTF, Priority-Preemptive, MLQ, IO-RR coming soon.
 */
public class SchedulerFactory {

    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

    static {
        DESCRIPTIONS.put("fcfs",       "First Come First Served (non-preemptive)  [DONE]");
        DESCRIPTIONS.put("sjf",        "Shortest Job First (non-preemptive)        [DONE]");
        DESCRIPTIONS.put("rr",         "Round Robin — requires quantum, e.g. 'rr 4' [DONE]");
        DESCRIPTIONS.put("priority",   "Priority Scheduling (non-preemptive)        [DONE]");
        DESCRIPTIONS.put("srtf",       "Shortest Remaining Time First  [COMING IN PHASE 3]");
        DESCRIPTIONS.put("priority-p", "Priority Scheduling Preemptive [COMING IN PHASE 3]");
        DESCRIPTIONS.put("mlq",        "Multilevel Queue                [COMING IN PHASE 3]");
    }

    private SchedulerFactory() {}

    public static Scheduler create(String key, int quantum) {
        return switch (key.toLowerCase()) {
            case "fcfs"       -> new FCFSScheduler();
            case "sjf"        -> new SJFScheduler();
            case "rr"         -> new RoundRobinScheduler(quantum);
            case "priority"   -> new PriorityScheduler(false);
            case "srtf", "priority-p", "mlq", "io-rr" ->
                throw new IllegalArgumentException(
                    "'" + key + "' is planned for Phase 3. Currently available: fcfs, sjf, rr, priority");
            default -> throw new IllegalArgumentException(
                    "Unknown scheduler: '" + key + "'. Available: fcfs, sjf, rr, priority");
        };
    }

    public static Scheduler create(String key) { return create(key, 1); }

    public static Set<String>        availableKeys()    { return DESCRIPTIONS.keySet(); }
    public static Map<String,String> getDescriptions()  { return DESCRIPTIONS; }
}
