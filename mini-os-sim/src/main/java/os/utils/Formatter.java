package os.utils;

import os.process.Process;
import os.scheduler.GanttEntry;
import os.scheduler.ScheduleResult;
import os.memory.MemoryBlock;

import java.util.List;

/**
 * Stateless utility class that renders simulation results as formatted
 * ASCII output for the CLI shell.
 *
 * All methods are static; this class is not intended to be instantiated.
 */
public final class Formatter {

    private Formatter() {}

    // ── ANSI colour helpers ──────────────────────────────────────────────────
    public static final String RESET  = "\u001B[0m";
    public static final String BOLD   = "\u001B[1m";
    public static final String CYAN   = "\u001B[36m";
    public static final String GREEN  = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED    = "\u001B[31m";
    public static final String BLUE   = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";

    private static String c(String colour, String text) { return colour + text + RESET; }
    public  static String bold(String s)   { return c(BOLD,   s); }
    public  static String cyan(String s)   { return c(CYAN,   s); }
    public  static String green(String s)  { return c(GREEN,  s); }
    public  static String yellow(String s) { return c(YELLOW, s); }
    public  static String red(String s)    { return c(RED,    s); }
    public  static String blue(String s)   { return c(BLUE,   s); }
    public  static String purple(String s) { return c(PURPLE, s); }

    // ── Gantt chart ──────────────────────────────────────────────────────────

    /**
     * Renders an ASCII Gantt chart.
     *
     * Each cell represents one Gantt entry; idle cells are shaded with "--".
     * Time markers are printed beneath the chart.
     *
     * Example:
     * ┌────────┬────────┬────────┐
     * │  P1    │  IDLE  │  P2    │
     * └────────┴────────┴────────┘
     *  0       4        6       10
     */
    public static String formatGantt(ScheduleResult result) {
        List<GanttEntry> gantt = result.getGanttChart();
        if (gantt.isEmpty()) return "(no gantt data)";

        StringBuilder top   = new StringBuilder();
        StringBuilder mid   = new StringBuilder();
        StringBuilder bot   = new StringBuilder();
        StringBuilder times = new StringBuilder();

        top.append("┌");
        bot.append("└");
        times.append(" ");

        for (int i = 0; i < gantt.size(); i++) {
            GanttEntry e   = gantt.get(i);
            int        dur = e.getDuration();
            // Cell width: at least 8, scaled by duration (capped so chart stays readable)
            int width = Math.max(8, Math.min(dur * 2, 16));

            String label = e.isIdle() ? "IDLE" : ("P" + e.getPid());
            String cell  = centerPad(label, width);

            top.append("─".repeat(width)).append(i < gantt.size() - 1 ? "┬" : "┐");
            mid.append("│").append(e.isIdle() ? yellow(cell) : cyan(cell));
            bot.append("─".repeat(width)).append(i < gantt.size() - 1 ? "┴" : "┘");

            String timeStr = String.valueOf(e.getStartTime());
            times.append(timeStr);
            times.append(" ".repeat(Math.max(1, width - timeStr.length() + 1)));
        }

        // Append the final time marker
        GanttEntry last = gantt.get(gantt.size() - 1);
        mid.append("│");
        times.append(last.getEndTime());

        return "\n" + top + "\n" + mid + "\n" + bot + "\n" + times + "\n";
    }

    // ── Per-process metrics table ────────────────────────────────────────────

    public static String formatProcessTable(ScheduleResult result) {
        List<Process> procs = result.getCompletedProcesses();
        StringBuilder sb    = new StringBuilder();

        String header = String.format(
                "%-5s %-12s %7s %7s %7s %9s %9s %9s",
                "PID", "Name", "Arrival", "Burst", "Priority",
                "CT", "TAT", "WT");
        sb.append("\n").append(bold(header)).append("\n");
        sb.append("─".repeat(75)).append("\n");

        for (Process p : procs) {
            sb.append(String.format(
                    "%-5d %-12s %7d %7d %7d %9d %9d %9d\n",
                    p.getPid(), p.getName(),
                    p.getArrivalTime(), p.getBurstTime(), p.getPriority(),
                    p.getCompletionTime(), p.getTurnaroundTime(), p.getWaitingTime()));
        }

        sb.append("─".repeat(75)).append("\n");
        sb.append(bold("Legend: "))
          .append("CT=Completion Time  TAT=Turnaround Time  WT=Waiting Time\n");
        return sb.toString();
    }

    // ── Aggregate metrics ────────────────────────────────────────────────────

    public static String formatMetrics(ScheduleResult result) {
        return String.format(
                "\n%s\n" +
                "  %-30s %s\n" +
                "  %-30s %s\n" +
                "  %-30s %s\n" +
                "  %-30s %s\n" +
                "  %-30s %s\n" +
                "  %-30s %s\n",
                bold("─── Performance Metrics ─────────────────────────────"),
                "Average Waiting Time:",      green(String.format("%.2f", result.getAvgWaitingTime())),
                "Average Turnaround Time:",   green(String.format("%.2f", result.getAvgTurnaroundTime())),
                "Average Response Time:",     green(String.format("%.2f", result.getAvgResponseTime())),
                "CPU Utilization:",           green(String.format("%.1f%%", result.getCpuUtilization() * 100)),
                "Throughput:",               green(String.format("%.4f proc/unit", result.getThroughput())),
                "Total Simulation Time:",     green(String.valueOf(result.getTotalTime())));
    }

    // ── Memory map ──────────────────────────────────────────────────────────

    public static String formatMemoryMap(List<MemoryBlock> blocks, int total) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(bold("─── Memory Map ──────────────────────────────────────")).append("\n");
        sb.append(String.format("%-6s %-6s %-6s %-6s %s\n",
                "Start", "End", "Size", "PID", "Status"));
        sb.append("─".repeat(50)).append("\n");

        int freeTotal = 0;
        for (MemoryBlock b : blocks) {
            String status = b.isAllocated()
                    ? green("USED  [" + b.getProcessName() + "]")
                    : yellow("FREE");
            sb.append(String.format("%-6d %-6d %-6d %-6s %s\n",
                    b.getStartAddress(), b.getEndAddress(), b.getSize(),
                    b.isAllocated() ? String.valueOf(b.getPid()) : "-",
                    status));
            if (!b.isAllocated()) freeTotal += b.getSize();
        }

        sb.append("─".repeat(50)).append("\n");
        sb.append(String.format("Total: %d u | Used: %d u | Free: %d u | Fragmentation: %.1f%%\n",
                total, total - freeTotal, freeTotal,
                total > 0 ? 100.0 * freeTotal / total : 0));
        return sb.toString();
    }

    // ── Process list ────────────────────────────────────────────────────────

    public static String formatProcessList(List<Process> procs) {
        if (procs.isEmpty()) return yellow("  (no processes defined)\n");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n%-5s %-12s %7s %7s %8s %-12s\n",
                "PID", "Name", "Arrival", "Burst", "Priority", "State"));
        sb.append("─".repeat(58)).append("\n");
        for (Process p : procs) {
            String stateStr = stateColour(p);
            sb.append(String.format("%-5d %-12s %7d %7d %8d %-20s\n",
                    p.getPid(), p.getName(),
                    p.getArrivalTime(), p.getBurstTime(), p.getPriority(),
                    stateStr));
        }
        return sb.toString();
    }

    private static String stateColour(Process p) {
        return switch (p.getState()) {
            case NEW        -> blue(p.getState().name());
            case READY      -> cyan(p.getState().name());
            case RUNNING    -> green(p.getState().name());
            case WAITING    -> yellow(p.getState().name());
            case TERMINATED -> red(p.getState().name());
        };
    }

    // ── Banner ───────────────────────────────────────────────────────────────

    public static String banner() {
        return CYAN +
                "╔══════════════════════════════════════════════════════╗\n" +
                "║        Mini OS Simulator  — CLI Shell v1.0           ║\n" +
                "║  Process · Scheduling · Memory · FileSystem          ║\n" +
                "╚══════════════════════════════════════════════════════╝\n"
                + RESET;
    }

    // ── Section header ───────────────────────────────────────────────────────

    public static String section(String title) {
        return "\n" + bold(CYAN + "┌─ " + title + " " + "─".repeat(Math.max(0, 50 - title.length())) + RESET) + "\n";
    }

    // ── Misc helpers ─────────────────────────────────────────────────────────

    private static String centerPad(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        int left  = (width - text.length()) / 2;
        int right = width - text.length() - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }

    public static String divider() {
        return "─".repeat(56) + "\n";
    }
}
