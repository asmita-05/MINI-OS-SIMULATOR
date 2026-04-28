package os.scheduler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Factory / registry for all scheduling algorithms.
 *
 * Provides a single point for constructing and looking up scheduler instances
 * by short-hand key.  Adding a new algorithm only requires registering it here.
 */
public class SchedulerFactory {

    // Ordered map so help text is printed in a consistent order
    private static final Map<String, String> DESCRIPTIONS = new LinkedHashMap<>();

    static {
        DESCRIPTIONS.put("fcfs",       "First Come First Served (non-preemptive)");
        DESCRIPTIONS.put("sjf",        "Shortest Job First (non-preemptive)");
        DESCRIPTIONS.put("srtf",       "Shortest Remaining Time First (preemptive SJF)");
        DESCRIPTIONS.put("rr",         "Round Robin — requires time quantum, e.g. 'rr 4'");
    }

    private SchedulerFactory() {} // utility class

    /**
     * Creates the scheduler identified by {@code key}.
     *
     * @param key     algorithm key (case-insensitive), e.g. "fcfs", "rr"
     * @param quantum time quantum for Round Robin (ignored for other algorithms)
     * @return the corresponding {@link Scheduler} instance
     * @throws IllegalArgumentException for unknown keys or invalid quantum
     */
    public static Scheduler create(String key, int quantum) {
        return switch (key.toLowerCase()) {
            case "fcfs"       -> new FCFSScheduler();
            case "sjf"        -> new SJFScheduler();
            case "srtf"       -> new SRTFScheduler();
            case "rr"         -> new RoundRobinScheduler(quantum);
            default           -> throw new IllegalArgumentException(
                    "Unknown scheduler: '" + key + "'. Use: " + availableKeys());
        };
    }

    /** Convenience overload for non-RR algorithms. */
    public static Scheduler create(String key) { return create(key, 1); }

    public static Set<String>       availableKeys()         { return DESCRIPTIONS.keySet(); }
    public static Map<String,String> getDescriptions()      { return DESCRIPTIONS; }
}
