package os.cli;

import os.process.Process;
import os.scheduler.ScheduleResult;
import os.scheduler.Scheduler;
import os.scheduler.SchedulerFactory;
import os.utils.CommandInput;
import os.utils.Formatter;

import java.util.*;

/**
 * Handles the 'compare' command.
 *
 * Runs all registered scheduling algorithms on the current process list and
 * renders a side-by-side summary table so the user can evaluate them at a
 * glance.  Optionally a subset of algorithms can be specified.
 *
 *   compare                      — compare all algorithms with default quantum
 *   compare fcfs sjf rr          — compare only the listed algorithms
 *   compare --quantum 3          — use quantum=3 for RR-based algorithms
 *
 * Output columns:
 *   Algorithm | Avg WT | Avg TAT | Avg RT | CPU Util | Throughput
 */
public class CompareCommandHandler implements CommandHandler {

    /** One result row per algorithm. */
    private static final class Row {
        final String  name;
        final double  awt;
        final double  atat;
        final double  art;
        final double  util;   // 0–100
        final double  tp;
        final boolean failed;

        Row(String name, double awt, double atat, double art,
            double util, double tp, boolean failed) {
            this.name   = name;
            this.awt    = awt;
            this.atat   = atat;
            this.art    = art;
            this.util   = util;
            this.tp     = tp;
            this.failed = failed;
        }
    }

    @Override
    public boolean handles(String command) {
        return command.equals("compare") || command.equals("cmp");
    }

    @Override
    public String execute(CommandInput input, SimulationContext ctx) {
        List<Process> procs = ctx.getProcessManager().getAll();
        if (procs.isEmpty())
            return Formatter.yellow("No processes defined. Use 'proc add' first.\n");

        // Parse optional quantum override and algorithm list
        int          quantum = ctx.getDefaultQuantum();
        List<String> keys    = new ArrayList<>();

        for (int i = 0; i < input.getArgCount(); i++) {
            String arg = input.getArg(i);
            if (arg.equals("--quantum") || arg.equals("-q")) {
                if (i + 1 < input.getArgCount()) {
                    quantum = input.getIntArg(i + 1, quantum);
                    i++;
                }
            } else {
                keys.add(arg.toLowerCase());
            }
        }

        if (keys.isEmpty()) keys.addAll(SchedulerFactory.availableKeys());

        List<Row> rows         = new ArrayList<>();
        int       finalQuantum = quantum;

        for (String key : keys) {
            try {
                Scheduler      s = SchedulerFactory.create(key, finalQuantum);
                ScheduleResult r = s.simulate(procs);
                rows.add(new Row(s.getName(),
                        r.getAvgWaitingTime(), r.getAvgTurnaroundTime(),
                        r.getAvgResponseTime(), r.getCpuUtilization() * 100.0,
                        r.getThroughput(), false));
            } catch (Exception e) {
                rows.add(new Row(key + " [" + e.getMessage() + "]",
                        0, 0, 0, 0, 0, true));
            }
        }

        return renderTable(rows, procs.size(), quantum);
    }

    private String renderTable(List<Row> rows, int procCount, int quantum) {
        double bestWT   = rows.stream().filter(r -> !r.failed).mapToDouble(r -> r.awt ).min().orElse(0);
        double bestTAT  = rows.stream().filter(r -> !r.failed).mapToDouble(r -> r.atat).min().orElse(0);
        double bestRT   = rows.stream().filter(r -> !r.failed).mapToDouble(r -> r.art ).min().orElse(0);
        double bestUtil = rows.stream().filter(r -> !r.failed).mapToDouble(r -> r.util).max().orElse(0);
        double bestTP   = rows.stream().filter(r -> !r.failed).mapToDouble(r -> r.tp  ).max().orElse(0);

        StringBuilder sb = new StringBuilder();
        sb.append(Formatter.section(
                "Algorithm Comparison (" + procCount + " processes, quantum=" + quantum + ")"));

        String hdr = String.format("%-40s %8s %8s %8s %8s %12s",
                "Algorithm", "Avg WT", "Avg TAT", "Avg RT", "CPU%", "Throughput");
        sb.append(Formatter.bold(hdr)).append("\n");
        sb.append("─".repeat(88)).append("\n");

        for (Row r : rows) {
            if (r.failed) {
                sb.append(Formatter.red(String.format("%-40s  FAILED\n", r.name)));
                continue;
            }
            sb.append(String.format("%-40s %8s %8s %8s %8s %12s\n",
                    r.name,
                    fmtNum(r.awt,  r.awt  == bestWT),
                    fmtNum(r.atat, r.atat == bestTAT),
                    fmtNum(r.art,  r.art  == bestRT),
                    fmtPct(r.util, r.util == bestUtil),
                    fmtTp (r.tp,   r.tp   == bestTP)));
        }

        sb.append("─".repeat(88)).append("\n");
        sb.append(Formatter.green("★") + " = best in category\n");
        return sb.toString();
    }

    private String fmtNum(double v, boolean best) {
        String s = String.format("%.2f", v);
        return best ? Formatter.green(s + "★") : s;
    }

    private String fmtPct(double v, boolean best) {
        String s = String.format("%.1f%%", v);
        return best ? Formatter.green(s + "★") : s;
    }

    private String fmtTp(double v, boolean best) {
        String s = String.format("%.4f", v);
        return best ? Formatter.green(s + "★") : s;
    }

    @Override
    public String helpText() {
        return "compare [algo...] [--quantum N]  — side-by-side metric comparison of schedulers";
    }
}
