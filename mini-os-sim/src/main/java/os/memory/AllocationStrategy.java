package os.memory;

/**
 * Contiguous memory allocation strategies for the memory manager.
 *
 * All three strategies scan the free-block list and select a hole to fill;
 * they differ only in which hole is chosen:
 *
 *  FIRST_FIT — the first hole large enough (fastest, low fragmentation overhead)
 *  BEST_FIT  — the smallest sufficient hole (minimises wasted space per allocation,
 *              but produces many tiny unusable fragments over time)
 *  WORST_FIT — the largest available hole (leaves bigger remainders that may be
 *              re-usable, but quickly exhausts large contiguous regions)
 */
public enum AllocationStrategy {
    FIRST_FIT("First-Fit"),
    BEST_FIT ("Best-Fit"),
    WORST_FIT("Worst-Fit");

    private final String displayName;

    AllocationStrategy(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }

    public static AllocationStrategy fromString(String s) {
        return switch (s.toLowerCase().replace("-", "_").replace(" ", "_")) {
            case "first_fit", "first"  -> FIRST_FIT;
            case "best_fit",  "best"   -> BEST_FIT;
            case "worst_fit", "worst"  -> WORST_FIT;
            default -> throw new IllegalArgumentException("Unknown strategy: " + s +
                    ". Use: first-fit, best-fit, worst-fit");
        };
    }
}
