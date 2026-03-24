package os.gui;

import os.cli.SimulationContext;
import os.process.Process;
import os.process.ProcessState;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Process Management panel.
 *
 * Left column  — form for creating processes
 * Right column — live process table with colour-coded state badges
 *
 * Controls: Add / Remove / Clear / Info
 */
public class ProcessPanel extends JPanel {

    private final SimulationContext ctx;

    // Form fields
    private JTextField  tfName;
    private JSpinner    spArrival, spBurst, spPriority;
    private JButton     btnAdd, btnRemove, btnClear;
    private JLabel      lblStatus;

    // Table
    private DefaultTableModel tableModel;
    private JTable            table;

    public ProcessPanel(SimulationContext ctx) {
        this.ctx = ctx;
        setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(Theme.GAP, Theme.GAP));
        setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        buildUI();
        refresh();
    }

    private void buildUI() {
        // ── Top: section header ──────────────────────────────────────────────
        add(UIHelper.sectionHeader("⬡  PROCESS MANAGEMENT"), BorderLayout.NORTH);

        // ── Centre: form (left) + table (right) ──────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildFormPanel(), buildTablePanel());
        split.setDividerLocation(280);
        split.setDividerSize(4);
        split.setBackground(Theme.BG_PANEL);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        // ── Bottom: status bar ───────────────────────────────────────────────
        lblStatus = UIHelper.muted("No processes. Use the form to add processes.");
        lblStatus.setBorder(new EmptyBorder(4, 0, 0, 0));
        add(lblStatus, BorderLayout.SOUTH);
    }

    // ── Form panel ────────────────────────────────────────────────────────────

    private JPanel buildFormPanel() {
        JPanel card = UIHelper.card();
        card.setLayout(new BorderLayout(0, Theme.GAP));
        card.setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        // Title
        JLabel title = UIHelper.heading("NEW PROCESS");
        card.add(title, BorderLayout.NORTH);

        // Form fields
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);

        tfName    = UIHelper.textField("e.g. httpd");
        spArrival = UIHelper.intSpinner(0, 0, 999);
        spBurst   = UIHelper.intSpinner(5, 1, 999);
        spPriority= UIHelper.intSpinner(0, 0, 99);

        fields.add(UIHelper.formRow("Name", tfName));
        fields.add(Box.createVerticalStrut(8));
        fields.add(UIHelper.formRow("Arrival", spArrival));
        fields.add(Box.createVerticalStrut(8));
        fields.add(UIHelper.formRow("Burst Time", spBurst));
        fields.add(Box.createVerticalStrut(8));
        fields.add(UIHelper.formRow("Priority", spPriority));
        fields.add(Box.createVerticalStrut(6));

        // Priority note
        JLabel note = UIHelper.muted("0 = highest priority");
        note.setBorder(new EmptyBorder(0, 94, 0, 0));
        fields.add(note);

        card.add(fields, BorderLayout.CENTER);

        // Buttons
        JPanel btns = new JPanel(new GridLayout(1, 1, 6, 0));
        btns.setOpaque(false);
        btnAdd = UIHelper.primaryButton("＋  ADD PROCESS");
        btnAdd.addActionListener(e -> addProcess());
        // Enter key shortcut
        tfName.addActionListener(e -> addProcess());
        btns.add(btnAdd);
        card.add(btns, BorderLayout.SOUTH);

        // Preset buttons
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        presets.setOpaque(false);
        presets.add(UIHelper.muted("Quick:"));
        for (String[] p : new String[][]{{"Alpha","0","10","3"},{"Beta","1","4","1"},{"Gamma","2","6","4"}}) {
            JButton pb = UIHelper.ghostButton(p[0]);
            pb.setFont(Theme.FONT_SMALL);
            pb.setPreferredSize(new Dimension(64, 24));
            String[] pf = p;
            pb.addActionListener(e -> {
                tfName.setText(pf[0]);
                spArrival.setValue(Integer.parseInt(pf[1]));
                spBurst.setValue(Integer.parseInt(pf[2]));
                spPriority.setValue(Integer.parseInt(pf[3]));
                addProcess();
            });
            presets.add(pb);
        }

        JPanel bottomWrap = new JPanel(new BorderLayout(0, 6));
        bottomWrap.setOpaque(false);
        bottomWrap.add(btns,    BorderLayout.NORTH);
        bottomWrap.add(presets, BorderLayout.CENTER);
        card.add(bottomWrap, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.BG_PANEL);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    // ── Table panel ───────────────────────────────────────────────────────────

    private JPanel buildTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0, Theme.GAP));
        p.setBackground(Theme.BG_PANEL);

        // Column setup
        String[] cols = {"PID", "Name", "Arrival", "Burst", "Priority", "Remaining", "State"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        // Custom state column renderer with colour badges
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,val,sel,focus,row,col);
                String state = val == null ? "" : val.toString();
                l.setFont(Theme.FONT_SMALL);
                l.setForeground(sel ? Theme.ACCENT_CYAN : Theme.stateColour(state));
                l.setBackground(sel ? Theme.BG_SELECTED : (row%2==0 ? Theme.BG_PANEL : Theme.BG_ROW_ALT));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(new EmptyBorder(2, 4, 2, 4));
                return l;
            }
        });

        // Alternating row colours
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,val,sel,focus,row,col);
                l.setFont(Theme.FONT_CODE);
                l.setForeground(sel ? Theme.ACCENT_CYAN : Theme.TEXT_PRIMARY);
                l.setBackground(sel ? Theme.BG_SELECTED : (row%2==0 ? Theme.BG_PANEL : Theme.BG_ROW_ALT));
                l.setBorder(new EmptyBorder(2, 6, 2, 6));
                return l;
            }
        });
        // Re-apply state renderer after default override
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,val,sel,focus,row,col);
                String state = val == null ? "" : val.toString();
                l.setFont(Theme.FONT_SMALL);
                l.setForeground(sel ? Theme.BG_DEEP : Theme.stateColour(state));
                l.setBackground(sel ? Theme.BG_SELECTED : Theme.alpha(Theme.stateColour(state), 25));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(new EmptyBorder(2, 4, 2, 4));
                l.setOpaque(true);
                return l;
            }
        });

        // Column widths
        int[] widths = {40, 100, 60, 60, 60, 70, 90};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane scroll = UIHelper.scrollPane(table);
        p.add(scroll, BorderLayout.CENTER);

        // Action buttons row
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actions.setBackground(Theme.BG_PANEL);
        btnRemove = UIHelper.dangerButton("✕  REMOVE");
        btnClear  = UIHelper.ghostButton("⊘  CLEAR ALL");
        btnRemove.addActionListener(e -> removeSelected());
        btnClear .addActionListener(e -> clearAll());
        actions.add(btnRemove);
        actions.add(btnClear);
        p.add(actions, BorderLayout.SOUTH);

        return p;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void addProcess() {
        String name = tfName.getText().trim();
        if (name.isEmpty()) { flashStatus("⚠ Enter a process name", Theme.ACCENT_AMBER); return; }

        int arrival  = (Integer) spArrival.getValue();
        int burst    = (Integer) spBurst.getValue();
        int priority = (Integer) spPriority.getValue();

        try {
            Process p = ctx.getProcessManager().createProcess(name, arrival, burst, priority);
            ctx.getProcessManager().admit(p);
            if (ctx.isAutoAllocMemory()) {
                int size  = ctx.getDefaultMemoryPerProc();
                int start = ctx.getMemoryManager().allocate(p.getPid(), name, size);
                if (start >= 0) { p.setMemoryStart(start); p.setMemorySize(size); }
            }
            tfName.setText("");
            refresh();
            flashStatus("✓ P" + p.getPid() + " [" + name + "] created", Theme.ACCENT_GREEN);
        } catch (IllegalArgumentException ex) {
            flashStatus("⚠ " + ex.getMessage(), Theme.ACCENT_RED);
        }
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { flashStatus("Select a process to remove", Theme.ACCENT_AMBER); return; }
        int pid = (Integer) tableModel.getValueAt(row, 0);
        boolean ok = ctx.getProcessManager().removeProcess(pid);
        refresh();
        flashStatus(ok ? "✓ P" + pid + " removed" : "⚠ Cannot remove running process",
                    ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
    }

    private void clearAll() {
        int count = ctx.getProcessManager().size();
        ctx.getProcessManager().clear();
        refresh();
        flashStatus("✓ Cleared " + count + " process(es)", Theme.ACCENT_AMBER);
    }

    // ── Refresh table ─────────────────────────────────────────────────────────

    public void refresh() {
        tableModel.setRowCount(0);
        List<Process> procs = ctx.getProcessManager().getAll();
        for (Process p : procs) {
            tableModel.addRow(new Object[]{
                p.getPid(), p.getName(),
                p.getArrivalTime(), p.getBurstTime(), p.getPriority(),
                p.getRemainingTime(), p.getState().name()
            });
        }
        int n = procs.size();
        lblStatus.setText(n == 0
                ? "No processes. Use the form to add processes."
                : n + " process" + (n==1?"":"es") + " defined  |  "
                  + procs.stream().filter(p->p.getState()==ProcessState.READY).count() + " READY");
    }

    private void flashStatus(String msg, Color colour) {
        lblStatus.setText(msg);
        lblStatus.setForeground(colour);
        javax.swing.Timer t = new javax.swing.Timer(3000, e -> {
            lblStatus.setForeground(Theme.TEXT_MUTED);
            refresh();
        });
        t.setRepeats(false);
        t.start();
    }
}
