package os.memory;

/**
 * Contiguous memory allocation strategies.
 * Phase 2: First-Fit implemented.
 * Phase 3: Best-Fit and Worst-Fit coming soon.
 */
public enum AllocationStrategy {
    FIRST_FIT("First-Fit  [DONE]"),
    BEST_FIT ("Best-Fit   [COMING IN PHASE 3]"),
    WORST_FIT("Worst-Fit  [COMING IN PHASE 3]");

    private final String displayName;
    AllocationStrategy(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }

    public static AllocationStrategy fromString(String s) {
        return switch (s.toLowerCase().replace("-","_").replace(" ","_")) {
            case "first_fit","first"  -> FIRST_FIT;
            case "best_fit","best"    -> {
                System.out.println("[Phase 3] Best-Fit not yet implemented. Using First-Fit.");
                yield FIRST_FIT;
            }
            case "worst_fit","worst"  -> {
                System.out.println("[Phase 3] Worst-Fit not yet implemented. Using First-Fit.");
                yield FIRST_FIT;
            }
            default -> throw new IllegalArgumentException("Unknown strategy: " + s);
        };
    }
}
