package os.gui;

import os.cli.SimulationContext;
import os.process.Process;
import os.scheduler.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * CPU Scheduling panel — Phase 2.
 * Implemented  : FCFS, SJF, Round Robin, Priority (non-preemptive).
 * Phase 3      : SRTF, Priority-Preemptive, MLQ, IO-RR, Compare All.
 */
public class SchedulerPanel extends JPanel {

    private final SimulationContext ctx;

    private JComboBox<String> cbAlgo;
    private JSpinner          spQuantum;
    private JButton           btnRun;
    private GanttChartPanel   ganttPanel;
    private DefaultTableModel metricsModel;
    private JTable            metricsTable;
    private JLabel            lblAvgWT, lblAvgTAT, lblAvgRT, lblUtil, lblThroughput, lblTotal;
    private JLabel            lblStatus;

    // Phase 2: only 4 algorithms available
    private static final String[] ALGO_KEYS = {"fcfs", "sjf", "rr", "priority"};
    private static final String[] ALGO_LABELS = {
        "FCFS — First Come First Served  [Phase 2]",
        "SJF  — Shortest Job First       [Phase 2]",
        "RR   — Round Robin              [Phase 2]",
        "Priority (Non-Preemptive)       [Phase 2]",
    };

    public SchedulerPanel(SimulationContext ctx) {
        this.ctx = ctx;
        setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(Theme.GAP, Theme.GAP));
        setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        buildUI();
    }

    private void buildUI() {
        add(UIHelper.sectionHeader("⬡  CPU SCHEDULING SIMULATOR"), BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);

        lblStatus = UIHelper.muted("Select an algorithm and press RUN SIMULATION");
        lblStatus.setBorder(new EmptyBorder(4, 0, 0, 0));
        add(lblStatus, BorderLayout.SOUTH);
    }

    private JPanel buildMainArea() {
        JPanel p = new JPanel(new BorderLayout(0, Theme.GAP));
        p.setBackground(Theme.BG_PANEL);
        p.add(buildControlBar(),    BorderLayout.NORTH);
        p.add(buildGanttSection(),  BorderLayout.CENTER);
        p.add(buildBottomSection(), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildControlBar() {
        JPanel bar = UIHelper.card();
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

        cbAlgo = UIHelper.comboBox(ALGO_LABELS);
        cbAlgo.setPreferredSize(new Dimension(300, 32));

        spQuantum = UIHelper.intSpinner(ctx.getDefaultQuantum(), 1, 100);
        spQuantum.setPreferredSize(new Dimension(68, 32));
        cbAlgo.addActionListener(e -> {
            boolean needsQ = ALGO_KEYS[Math.max(0, cbAlgo.getSelectedIndex())].equals("rr");
            spQuantum.setEnabled(needsQ);
        });

        btnRun = UIHelper.successButton("▶  RUN SIMULATION");
        btnRun.setPreferredSize(new Dimension(180, 36));
        btnRun.addActionListener(e -> runSimulation());

        // Phase 3 coming soon badge
        JLabel p3badge = UIHelper.badge("Phase 3: SRTF · MLQ · Priority-P · IO-RR · Compare", Theme.ACCENT_AMBER);

        bar.add(UIHelper.body("Algorithm:"));
        bar.add(cbAlgo);
        bar.add(UIHelper.body("Quantum:"));
        bar.add(spQuantum);
        bar.add(btnRun);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(p3badge);
        return bar;
    }

    private JPanel buildGanttSection() {
        JPanel p = UIHelper.card();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));
        p.add(UIHelper.heading("CPU TIMELINE  (Gantt Chart)"), BorderLayout.NORTH);
        ganttPanel = new GanttChartPanel();
        ganttPanel.setPreferredSize(new Dimension(800, 110));
        JScrollPane scroll = UIHelper.scrollPane(ganttPanel);
        scroll.setPreferredSize(new Dimension(800, 130));
        scroll.setBorder(null);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildBottomSection() {
        JPanel p = new JPanel(new GridLayout(1, 2, Theme.GAP, 0));
        p.setBackground(Theme.BG_PANEL);
        p.setPreferredSize(new Dimension(800, 200));
        p.add(buildProcessTable());
        p.add(buildMetricCards());
        return p;
    }

    private JPanel buildProcessTable() {
        JPanel p = UIHelper.card();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));
        p.add(UIHelper.heading("PER-PROCESS METRICS"), BorderLayout.NORTH);

        String[] cols = {"PID", "Name", "CT", "TAT", "WT", "RT"};
        metricsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        metricsTable = new JTable(metricsModel);
        UIHelper.styleTable(metricsTable);
        metricsTable.setRowHeight(24);
        metricsTable.setFont(Theme.FONT_SMALL);
        metricsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                l.setFont(Theme.FONT_SMALL);
                l.setForeground(sel ? Theme.ACCENT_CYAN : Theme.TEXT_PRIMARY);
                l.setBackground(sel ? Theme.BG_SELECTED : (row%2==0 ? Theme.BG_PANEL : Theme.BG_ROW_ALT));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(new EmptyBorder(1,4,1,4));
                return l;
            }
        });
        p.add(UIHelper.scrollPane(metricsTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildMetricCards() {
        JPanel p = UIHelper.card();
        p.setLayout(new GridLayout(3, 2, 8, 8));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        lblAvgWT      = metricVal("—", Theme.ACCENT_CYAN);
        lblAvgTAT     = metricVal("—", Theme.ACCENT_TEAL);
        lblAvgRT      = metricVal("—", Theme.ACCENT_PURPLE);
        lblUtil       = metricVal("—", Theme.ACCENT_GREEN);
        lblThroughput = metricVal("—", Theme.ACCENT_AMBER);
        lblTotal      = metricVal("—", Theme.TEXT_SECONDARY);

        p.add(metricCard("AVG WAIT TIME",  lblAvgWT));
        p.add(metricCard("AVG TURNAROUND", lblAvgTAT));
        p.add(metricCard("AVG RESPONSE",   lblAvgRT));
        p.add(metricCard("CPU UTILIZATION",lblUtil));
        p.add(metricCard("THROUGHPUT",     lblThroughput));
        p.add(metricCard("TOTAL TIME",     lblTotal));
        return p;
    }

    private JLabel metricVal(String t, Color c) {
        JLabel l = new JLabel(t, SwingConstants.CENTER);
        l.setFont(Theme.FONT_MONO_LG); l.setForeground(c); return l;
    }

    private JPanel metricCard(String title, JLabel val) {
        JPanel p = new JPanel(new BorderLayout(0, 2)) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_INPUT);
                g2.fillRoundRect(0,0,getWidth()-1,getHeight()-1,4,4);
                g2.setColor(Theme.BORDER_DIM);
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,4,4);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6,8,6,8));
        JLabel t = new JLabel(title, SwingConstants.CENTER);
        t.setFont(Theme.FONT_SMALL); t.setForeground(Theme.TEXT_MUTED);
        p.add(t, BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private void runSimulation() {
        List<Process> procs = ctx.getProcessManager().getAll();
        if (procs.isEmpty()) {
            flashStatus("⚠ No processes. Go to Process tab and add some.", Theme.ACCENT_AMBER);
            return;
        }
        int    idx     = cbAlgo.getSelectedIndex();
        String key     = ALGO_KEYS[Math.max(0, idx)];
        int    quantum = (Integer) spQuantum.getValue();

        try {
            Scheduler      s = SchedulerFactory.create(key, quantum);
            ScheduleResult r = s.simulate(procs);
            ctx.setScheduler(key, quantum);

            ganttPanel.setResult(r);
            metricsModel.setRowCount(0);
            for (Process p : r.getCompletedProcesses()) {
                metricsModel.addRow(new Object[]{
                    p.getPid(), p.getName(),
                    p.getCompletionTime(), p.getTurnaroundTime(),
                    p.getWaitingTime(), p.getResponseTime()
                });
            }
            lblAvgWT     .setText(String.format("%.2f", r.getAvgWaitingTime()));
            lblAvgTAT    .setText(String.format("%.2f", r.getAvgTurnaroundTime()));
            lblAvgRT     .setText(String.format("%.2f", r.getAvgResponseTime()));
            lblUtil      .setText(String.format("%.1f%%", r.getCpuUtilization()*100));
            lblThroughput.setText(String.format("%.4f", r.getThroughput()));
            lblTotal     .setText(String.valueOf(r.getTotalTime()));

            flashStatus("✓ Simulation complete — " + s.getName(), Theme.ACCENT_GREEN);
        } catch (Exception ex) {
            flashStatus("⚠ " + ex.getMessage(), Theme.ACCENT_RED);
        }
    }

    private void flashStatus(String msg, Color col) {
        lblStatus.setText(msg); lblStatus.setForeground(col);
        javax.swing.Timer t = new javax.swing.Timer(5000, e -> {
            lblStatus.setForeground(Theme.TEXT_MUTED);
            lblStatus.setText("Select an algorithm and press RUN SIMULATION");
        });
        t.setRepeats(false); t.start();
    }
}
