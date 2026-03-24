package os.gui;

import os.cli.SimulationContext;
import os.memory.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * Memory Management panel — Phase 2.
 *
 * Implemented : First-Fit contiguous allocation + visual memory bar.
 * Phase 3     : Best-Fit, Worst-Fit, Paging memory model.
 */
public class MemoryPanel extends JPanel {

    private final SimulationContext ctx;

    private JSpinner          spMemSize;
    private JSpinner          spAllocPid, spAllocSize;
    private JTextField        tfAllocName;
    private JButton           btnInit, btnAlloc, btnFree;
    private JSpinner          spFreePid;
    private JLabel            lblStatus;
    private MemoryMapPanel    memMapPanel;
    private DefaultTableModel tableModel;
    private JTable            table;
    private JLabel            lblTotal, lblUsed, lblFree;

    public MemoryPanel(SimulationContext ctx) {
        this.ctx = ctx;
        setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(Theme.GAP, Theme.GAP));
        setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        buildUI();
        refresh();
    }

    private void buildUI() {
        add(UIHelper.sectionHeader("⬡  MEMORY MANAGEMENT"), BorderLayout.NORTH);

        JPanel centre = new JPanel(new BorderLayout(0, Theme.GAP));
        centre.setBackground(Theme.BG_PANEL);
        centre.add(buildControlBar(),  BorderLayout.NORTH);
        centre.add(buildMapSection(),  BorderLayout.CENTER);
        centre.add(buildTableSection(),BorderLayout.SOUTH);
        add(centre, BorderLayout.CENTER);

        // Phase 3 coming soon banner
        JPanel phase3 = buildPhase3Banner();
        add(phase3, BorderLayout.EAST);

        lblStatus = UIHelper.muted("Initialise memory, then allocate blocks.");
        lblStatus.setBorder(new EmptyBorder(4,0,0,0));
        add(lblStatus, BorderLayout.SOUTH);
    }

    private JPanel buildPhase3Banner() {
        JPanel p = UIHelper.card();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(14, 14, 14, 14));
        p.setPreferredSize(new Dimension(200, 0));

        JLabel title = UIHelper.label("PHASE 3", Theme.FONT_SUBHEAD, Theme.ACCENT_AMBER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(8));

        JLabel sub = UIHelper.label("Coming Soon", Theme.FONT_SMALL, Theme.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(sub);
        p.add(Box.createVerticalStrut(16));

        String[] planned = {
            "Best-Fit strategy",
            "Worst-Fit strategy",
            "Paging model",
            "Page tables",
            "Address translation",
            "Page fault detection",
            "Fragmentation analysis",
        };
        for (String item : planned) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            row.setBackground(Theme.BG_CARD);
            row.setMaximumSize(new Dimension(200, 22));
            JLabel dot = UIHelper.label("○", Theme.FONT_SMALL, Theme.ACCENT_AMBER);
            JLabel lbl = UIHelper.label(item, Theme.FONT_SMALL, Theme.TEXT_MUTED);
            row.add(dot); row.add(lbl);
            p.add(row);
        }
        return p;
    }

    private JPanel buildControlBar() {
        JPanel bar = UIHelper.card();
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

        spMemSize = UIHelper.intSpinner(1024, 64, 65536);
        spMemSize.setPreferredSize(new Dimension(90, 30));

        btnInit = UIHelper.primaryButton("INIT MEMORY  (First-Fit)");
        btnInit.addActionListener(e -> initMemory());

        JCheckBox cbAuto = new JCheckBox("Auto-alloc on proc add");
        cbAuto.setFont(Theme.FONT_SMALL);
        cbAuto.setForeground(Theme.TEXT_SECONDARY);
        cbAuto.setBackground(Theme.BG_CARD);
        cbAuto.setSelected(ctx.isAutoAllocMemory());
        cbAuto.addActionListener(e -> ctx.setAutoAllocMemory(cbAuto.isSelected()));

        JSpinner spPer = UIHelper.intSpinner(ctx.getDefaultMemoryPerProc(), 1, 4096);
        spPer.setPreferredSize(new Dimension(70, 28));
        spPer.addChangeListener(e -> ctx.setDefaultMemoryPerProc((Integer) spPer.getValue()));

        bar.add(UIHelper.body("Size:"));
        bar.add(spMemSize);
        bar.add(UIHelper.badge("First-Fit Only", Theme.ACCENT_AMBER));
        bar.add(btnInit);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(cbAuto);
        bar.add(UIHelper.body("Per proc:"));
        bar.add(spPer);
        return bar;
    }

    private JPanel buildMapSection() {
        JPanel outer = new JPanel(new BorderLayout(0, Theme.GAP));
        outer.setBackground(Theme.BG_PANEL);

        JPanel mapCard = UIHelper.card();
        mapCard.setLayout(new BorderLayout(0, 6));
        mapCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        mapCard.add(UIHelper.heading("PHYSICAL ADDRESS SPACE"), BorderLayout.NORTH);
        memMapPanel = new MemoryMapPanel();
        memMapPanel.setPreferredSize(new Dimension(600, 100));
        JScrollPane ms = UIHelper.scrollPane(memMapPanel);
        ms.setPreferredSize(new Dimension(600, 120));
        ms.setBorder(null);
        mapCard.add(ms, BorderLayout.CENTER);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        stats.setBackground(Theme.BG_CARD);
        stats.setBorder(new EmptyBorder(6, 12, 6, 12));
        lblTotal = statLabel("—"); lblUsed = statLabel("—"); lblFree = statLabel("—");
        stats.add(statUnit("TOTAL", lblTotal, Theme.TEXT_SECONDARY));
        stats.add(statUnit("USED",  lblUsed,  Theme.ACCENT_CYAN));
        stats.add(statUnit("FREE",  lblFree,  Theme.ACCENT_GREEN));
        mapCard.add(stats, BorderLayout.SOUTH);
        outer.add(mapCard, BorderLayout.CENTER);
        outer.add(buildAllocPanel(), BorderLayout.EAST);
        return outer;
    }

    private JLabel statLabel(String t) {
        JLabel l = new JLabel(t); l.setFont(Theme.FONT_MONO_LG); l.setForeground(Theme.TEXT_PRIMARY); return l;
    }
    private JPanel statUnit(String title, JLabel val, Color col) {
        JPanel p = new JPanel(new BorderLayout(0,2)); p.setBackground(Theme.BG_CARD);
        val.setForeground(col);
        p.add(UIHelper.muted(title), BorderLayout.NORTH);
        p.add(val, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildAllocPanel() {
        JPanel card = UIHelper.card();
        card.setLayout(new BorderLayout(0, Theme.GAP));
        card.setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        card.setPreferredSize(new Dimension(220, 10));
        card.add(UIHelper.heading("ALLOCATE"), BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);

        spAllocPid  = UIHelper.intSpinner(1, 1, 9999);
        tfAllocName = UIHelper.textField("name");
        spAllocSize = UIHelper.intSpinner(64, 1, 65536);
        spFreePid   = UIHelper.intSpinner(1, 1, 9999);

        fields.add(UIHelper.formRow("PID",   spAllocPid));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("Name",  tfAllocName));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("Size",  spAllocSize));
        fields.add(Box.createVerticalStrut(10));
        btnAlloc = UIHelper.primaryButton("⊕  ALLOCATE");
        btnAlloc.addActionListener(e -> allocate());
        btnAlloc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        fields.add(btnAlloc);
        fields.add(Box.createVerticalStrut(12));
        fields.add(UIHelper.separator());
        fields.add(Box.createVerticalStrut(8));
        fields.add(UIHelper.formRow("Free PID", spFreePid));
        fields.add(Box.createVerticalStrut(6));
        btnFree = UIHelper.dangerButton("⊖  FREE");
        btnFree.addActionListener(e -> freeMemory());
        btnFree.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        fields.add(btnFree);

        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTableSection() {
        JPanel p = UIHelper.card();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));
        p.setPreferredSize(new Dimension(0, 160));
        p.add(UIHelper.heading("MEMORY BLOCK TABLE"), BorderLayout.NORTH);

        String[] cols = {"Start", "End", "Size", "PID", "Name", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UIHelper.styleTable(table);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                l.setFont(Theme.FONT_SMALL);
                boolean used = val != null && val.toString().startsWith("USED");
                l.setForeground(sel ? Theme.BG_DEEP : (used ? Theme.ACCENT_GREEN : Theme.TEXT_MUTED));
                l.setBackground(sel ? Theme.BG_SELECTED : Theme.alpha(used ? Theme.ACCENT_GREEN : Theme.BORDER_DIM, 22));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(new EmptyBorder(1,4,1,4));
                l.setOpaque(true);
                return l;
            }
        });
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                l.setFont(Theme.FONT_SMALL);
                l.setForeground(sel ? Theme.ACCENT_CYAN : Theme.TEXT_PRIMARY);
                l.setBackground(sel ? Theme.BG_SELECTED : (row%2==0 ? Theme.BG_PANEL : Theme.BG_ROW_ALT));
                l.setBorder(new EmptyBorder(1,6,1,6));
                return l;
            }
        });
        p.add(UIHelper.scrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void initMemory() {
        int size = (Integer) spMemSize.getValue();
        ctx.reinitMemory(size, AllocationStrategy.FIRST_FIT);
        memMapPanel.reset();
        refresh();
        flashStatus("✓ Memory initialised: " + size + " units, First-Fit.", Theme.ACCENT_GREEN);
    }

    private void allocate() {
        int pid = (Integer) spAllocPid.getValue();
        String name = tfAllocName.getText().trim();
        if (name.isEmpty()) name = "P" + pid;
        int size  = (Integer) spAllocSize.getValue();
        int start = ctx.getMemoryManager().allocate(pid, name, size);
        if (start < 0) {
            flashStatus("⚠ Allocation failed — not enough free memory.", Theme.ACCENT_RED);
        } else {
            ctx.getProcessManager().findByPid(pid).ifPresent(p -> {
                p.setMemoryStart(start); p.setMemorySize(size);
            });
            refresh();
            flashStatus("✓ P" + pid + " allocated " + size + " units at " + start, Theme.ACCENT_GREEN);
        }
    }

    private void freeMemory() {
        int pid = (Integer) spFreePid.getValue();
        boolean freed = ctx.getMemoryManager().free(pid);
        ctx.getProcessManager().findByPid(pid).ifPresent(p -> {
            p.setMemoryStart(-1); p.setMemorySize(0);
        });
        refresh();
        flashStatus(freed ? "✓ P" + pid + " memory freed" : "⚠ No memory for P" + pid,
                    freed ? Theme.ACCENT_GREEN : Theme.ACCENT_AMBER);
    }

    public void refresh() {
        MemoryManager mm = ctx.getMemoryManager();
        List<MemoryBlock> blocks = mm.getMemoryMap();
        memMapPanel.setBlocks(blocks, mm.getTotalMemory());
        lblTotal.setText(mm.getTotalMemory() + " u");
        lblUsed .setText(mm.getUsedMemory()  + " u");
        lblFree .setText(mm.getFreeMemory()  + " u");
        tableModel.setRowCount(0);
        for (MemoryBlock b : blocks) {
            tableModel.addRow(new Object[]{
                b.getStartAddress(), b.getEndAddress(), b.getSize(),
                b.isAllocated() ? b.getPid() : "-",
                b.isAllocated() ? b.getProcessName() : "",
                b.isAllocated() ? "USED [" + b.getProcessName() + "]" : "FREE"
            });
        }
    }

    private void flashStatus(String msg, Color col) {
        lblStatus.setText(msg); lblStatus.setForeground(col);
        javax.swing.Timer t = new javax.swing.Timer(4000, e -> {
            lblStatus.setForeground(Theme.TEXT_MUTED);
            lblStatus.setText("Initialise memory, then allocate blocks.");
        });
        t.setRepeats(false); t.start();
    }
}
