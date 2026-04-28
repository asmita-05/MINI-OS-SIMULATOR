package os.gui;

import os.cli.SimulationContext;
import os.process.Process;
import os.scheduler.ScheduleResult;
import os.scheduler.Scheduler;
import os.scheduler.SchedulerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * CPU Scheduling panel.
 */
public class SchedulerPanel extends JPanel {

    private final SimulationContext ctx;

    private JComboBox<String> cbAlgo;
    private JSpinner spQuantum;
    private JLabel lblAlgoName;
    private JButton btnRun;
    private JButton btnReplay;
    private GanttChartPanel ganttPanel;
    private DefaultTableModel metricsModel;
    private JTable metricsTable;
    private JLabel lblAvgWT, lblAvgTAT, lblAvgRT, lblUtil, lblThroughput, lblTotal;
    private JLabel lblStatus;
    private ScheduleResult lastResult;

    private static final String[] ALGO_KEYS = {
        "fcfs", "sjf", "srtf", "rr"
    };
    private static final String[] ALGO_LABELS = {
        "FCFS - First Come First Served",
        "SJF - Shortest Job First",
        "SRTF - Shortest Remaining Time",
        "RR - Round Robin",
    };

    public SchedulerPanel(SimulationContext ctx) {
        this.ctx = ctx;
        setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(Theme.GAP, Theme.GAP));
        setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        buildUI();
    }

    private void buildUI() {
        add(UIHelper.sectionHeader("CPU SCHEDULING"), BorderLayout.NORTH);
        add(buildMainArea(), BorderLayout.CENTER);

        lblStatus = UIHelper.muted("Select an algorithm and press RUN SIMULATION");
        lblStatus.setBorder(new EmptyBorder(4, 0, 0, 0));
        add(lblStatus, BorderLayout.SOUTH);
    }

    private JPanel buildMainArea() {
        JPanel p = new JPanel(new BorderLayout(0, Theme.GAP));
        p.setBackground(Theme.BG_PANEL);
        p.add(buildControlBar(), BorderLayout.NORTH);
        p.add(buildGanttSection(), BorderLayout.CENTER);
        p.add(buildMetricsSection(), BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildControlBar() {
        JPanel bar = UIHelper.card();
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

        cbAlgo = UIHelper.comboBox(ALGO_LABELS);
        cbAlgo.setPreferredSize(new Dimension(270, 32));
        cbAlgo.addActionListener(e -> updateAlgoName());

        JLabel qLabel = UIHelper.body("Quantum:");
        spQuantum = UIHelper.intSpinner(ctx.getDefaultQuantum(), 1, 100);
        spQuantum.setPreferredSize(new Dimension(68, 32));

        lblAlgoName = UIHelper.badge("FCFS", Theme.ACCENT_CYAN);
        lblAlgoName.setPreferredSize(new Dimension(140, 24));

        btnRun = UIHelper.successButton("RUN SIMULATION");
        btnRun.setPreferredSize(new Dimension(180, 36));
        btnRun.setFont(Theme.FONT_SUBHEAD);
        btnRun.addActionListener(e -> runSimulation());

        btnReplay = UIHelper.primaryButton("LIVE REPLAY");
        btnReplay.setPreferredSize(new Dimension(150, 36));
        btnReplay.setFont(Theme.FONT_SUBHEAD);
        btnReplay.addActionListener(e -> replayLastSchedule());

        bar.add(UIHelper.body("Algorithm:"));
        bar.add(cbAlgo);
        bar.add(qLabel);
        bar.add(spQuantum);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(btnRun);
        bar.add(btnReplay);
        bar.add(lblAlgoName);

        updateAlgoName();
        return bar;
    }

    private void updateAlgoName() {
        int idx = cbAlgo.getSelectedIndex();
        String[] shortNames = {"FCFS", "SJF", "SRTF", "RR"};
        if (idx >= 0 && idx < shortNames.length) {
            lblAlgoName.setText(" " + shortNames[idx] + " ");
        }
        spQuantum.setEnabled(ALGO_KEYS[Math.max(0, idx)].equals("rr"));
    }

    private JPanel buildGanttSection() {
        JPanel p = UIHelper.card();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        p.add(UIHelper.heading("CPU TIMELINE"), BorderLayout.NORTH);

        ganttPanel = new GanttChartPanel();
        ganttPanel.setPreferredSize(new Dimension(800, 110));
        JScrollPane scroll = UIHelper.scrollPane(ganttPanel);
        scroll.setPreferredSize(new Dimension(800, 130));
        scroll.setBorder(null);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildMetricsSection() {
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
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        metricsTable = new JTable(metricsModel);
        UIHelper.styleTable(metricsTable);
        metricsTable.setRowHeight(24);
        metricsTable.setFont(Theme.FONT_SMALL);

        metricsTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                l.setFont(Theme.FONT_SMALL);
                int wt = val instanceof Integer ? (Integer) val : 0;
                l.setForeground(sel ? Theme.ACCENT_CYAN : (wt > 15 ? Theme.ACCENT_RED : wt > 7 ? Theme.ACCENT_AMBER : Theme.ACCENT_GREEN));
                l.setBackground(sel ? Theme.BG_SELECTED : (row % 2 == 0 ? Theme.BG_PANEL : Theme.BG_ROW_ALT));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(new EmptyBorder(1, 4, 1, 4));
                return l;
            }
        });

        metricsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                l.setFont(Theme.FONT_SMALL);
                l.setForeground(sel ? Theme.ACCENT_CYAN : Theme.TEXT_PRIMARY);
                l.setBackground(sel ? Theme.BG_SELECTED : (row % 2 == 0 ? Theme.BG_PANEL : Theme.BG_ROW_ALT));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(new EmptyBorder(1, 4, 1, 4));
                return l;
            }
        });

        int[] cw = {36, 90, 40, 40, 40, 40};
        for (int i = 0; i < cw.length; i++) {
            metricsTable.getColumnModel().getColumn(i).setPreferredWidth(cw[i]);
        }
        p.add(UIHelper.scrollPane(metricsTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildMetricCards() {
        JPanel p = UIHelper.card();
        p.setLayout(new GridLayout(3, 2, 8, 8));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));

        lblAvgWT = metricLabel("-", Theme.ACCENT_CYAN);
        lblAvgTAT = metricLabel("-", Theme.ACCENT_TEAL);
        lblAvgRT = metricLabel("-", Theme.ACCENT_PURPLE);
        lblUtil = metricLabel("-", Theme.ACCENT_GREEN);
        lblThroughput = metricLabel("-", Theme.ACCENT_AMBER);
        lblTotal = metricLabel("-", Theme.TEXT_SECONDARY);

        p.add(metricCard("AVG WAIT TIME", lblAvgWT));
        p.add(metricCard("AVG TURNAROUND", lblAvgTAT));
        p.add(metricCard("AVG RESPONSE", lblAvgRT));
        p.add(metricCard("CPU UTILIZATION", lblUtil));
        p.add(metricCard("THROUGHPUT", lblThroughput));
        p.add(metricCard("TOTAL TIME", lblTotal));
        return p;
    }

    private JLabel metricLabel(String text, Color col) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.FONT_MONO_LG);
        l.setForeground(col);
        return l;
    }

    private JPanel metricCard(String title, JLabel valueLabel) {
        JPanel p = new JPanel(new BorderLayout(0, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2.setColor(Theme.BORDER_DIM);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 8, 6, 8));
        JLabel tl = new JLabel(title, SwingConstants.CENTER);
        tl.setFont(Theme.FONT_SMALL);
        tl.setForeground(Theme.TEXT_MUTED);
        p.add(tl, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    private void runSimulation() {
        List<Process> procs = ctx.getProcessManager().getAll();
        if (procs.isEmpty()) {
            flashStatus("No processes defined. Add some first.", Theme.ACCENT_AMBER);
            return;
        }

        int idx = cbAlgo.getSelectedIndex();
        String key = ALGO_KEYS[Math.max(0, idx)];
        int quantum = (Integer) spQuantum.getValue();

        try {
            Scheduler s = SchedulerFactory.create(key, quantum);
            ctx.setScheduler(key, quantum);
            ScheduleResult result = s.simulate(procs);
            lastResult = result;

            ganttPanel.setResult(result);
            metricsModel.setRowCount(0);
            for (Process p : result.getCompletedProcesses()) {
                metricsModel.addRow(new Object[]{
                    p.getPid(), p.getName(),
                    p.getCompletionTime(), p.getTurnaroundTime(),
                    p.getWaitingTime(), p.getResponseTime()
                });
            }

            lblAvgWT.setText(String.format("%.2f", result.getAvgWaitingTime()));
            lblAvgTAT.setText(String.format("%.2f", result.getAvgTurnaroundTime()));
            lblAvgRT.setText(String.format("%.2f", result.getAvgResponseTime()));
            lblUtil.setText(String.format("%.1f%%", result.getCpuUtilization() * 100));
            lblThroughput.setText(String.format("%.4f", result.getThroughput()));
            lblTotal.setText(String.valueOf(result.getTotalTime()));
            startReplay(result, "Simulation complete - " + s.getName());
        } catch (Exception ex) {
            flashStatus(ex.getMessage(), Theme.ACCENT_RED);
        }
    }

    private void replayLastSchedule() {
        if (lastResult == null) {
            flashStatus("Run a simulation first to create a live schedule.", Theme.ACCENT_AMBER);
            return;
        }
        startReplay(lastResult, "Replaying " + ctx.getActiveScheduler().getName());
    }

    private void startReplay(ScheduleResult result, String successMessage) {
        btnRun.setEnabled(false);
        btnReplay.setEnabled(false);
        flashStatus("Dispatching live Java threads...", Theme.ACCENT_CYAN);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                ctx.getLiveSchedulerController().replayBlocking(result);
                return null;
            }

            @Override
            protected void done() {
                btnRun.setEnabled(true);
                btnReplay.setEnabled(true);
                flashStatus(successMessage, Theme.ACCENT_GREEN);
            }
        };
        worker.execute();
    }

    private void flashStatus(String msg, Color col) {
        lblStatus.setText(msg);
        lblStatus.setForeground(col);
        javax.swing.Timer t = new javax.swing.Timer(5000, e -> {
            lblStatus.setForeground(Theme.TEXT_MUTED);
            lblStatus.setText("Select an algorithm and press RUN SIMULATION");
        });
        t.setRepeats(false);
        t.start();
    }
}
