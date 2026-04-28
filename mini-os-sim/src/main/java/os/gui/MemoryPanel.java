package os.gui;

import os.cli.SimulationContext;
import os.memory.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Memory Management panel.
 */
public class MemoryPanel extends JPanel {

    private final SimulationContext ctx;

    private JSpinner spMemSize;
    private JComboBox<String> cbStrategy;
    private JButton btnInit;
    private JSpinner spAllocPid, spAllocSize;
    private JTextField tfAllocName;
    private JButton btnAlloc, btnFree;
    private JSpinner spFreePid;
    private JLabel lblStatus;

    private MemoryMapPanel memMapPanel;

    private DefaultTableModel tableModel;
    private JTable table;

    private JLabel lblTotal, lblUsed, lblFree, lblFrag;

    public MemoryPanel(SimulationContext ctx) {
        this.ctx = ctx;
        setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(Theme.GAP, Theme.GAP));
        setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        buildUI();
        refresh();
    }

    private void buildUI() {
        add(UIHelper.sectionHeader("MEMORY MANAGEMENT"), BorderLayout.NORTH);

        JPanel centre = new JPanel(new BorderLayout(0, Theme.GAP));
        centre.setBackground(Theme.BG_PANEL);
        centre.add(buildTopRow(), BorderLayout.NORTH);
        centre.add(buildMapSection(), BorderLayout.CENTER);
        centre.add(buildBottomRow(), BorderLayout.SOUTH);
        add(centre, BorderLayout.CENTER);

        lblStatus = UIHelper.muted("Initialise memory, then allocate blocks to processes.");
        lblStatus.setBorder(new EmptyBorder(4, 0, 0, 0));
        add(lblStatus, BorderLayout.SOUTH);
    }

    private JPanel buildTopRow() {
        JPanel card = UIHelper.card();
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 10));

        spMemSize = UIHelper.intSpinner(1024, 64, 65536);
        spMemSize.setPreferredSize(new Dimension(90, 30));

        cbStrategy = UIHelper.comboBox(new String[]{"first-fit", "best-fit", "worst-fit"});
        cbStrategy.setPreferredSize(new Dimension(130, 30));

        btnInit = UIHelper.primaryButton("INIT MEMORY");
        btnInit.addActionListener(e -> initMemory());

        card.add(UIHelper.body("Size:"));
        card.add(spMemSize);
        card.add(UIHelper.body("Strategy:"));
        card.add(cbStrategy);
        card.add(btnInit);
        return card;
    }

    private JPanel buildMapSection() {
        JPanel outer = new JPanel(new BorderLayout(0, Theme.GAP));
        outer.setBackground(Theme.BG_PANEL);

        JPanel mapCard = UIHelper.card();
        mapCard.setLayout(new BorderLayout(0, 6));
        mapCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        mapCard.add(UIHelper.heading("PHYSICAL ADDRESS SPACE"), BorderLayout.NORTH);
        memMapPanel = new MemoryMapPanel();
        memMapPanel.setPreferredSize(new Dimension(700, 110));
        JScrollPane mapScroll = UIHelper.scrollPane(memMapPanel);
        mapScroll.setPreferredSize(new Dimension(700, 130));
        mapScroll.setBorder(null);
        mapCard.add(mapScroll, BorderLayout.CENTER);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        stats.setBackground(Theme.BG_CARD);
        stats.setBorder(new EmptyBorder(6, 12, 6, 12));
        lblTotal = statLabel("-");
        lblUsed = statLabel("-");
        lblFree = statLabel("-");
        lblFrag = statLabel("-");
        stats.add(statUnit("TOTAL", lblTotal, Theme.TEXT_SECONDARY));
        stats.add(statUnit("USED", lblUsed, Theme.ACCENT_CYAN));
        stats.add(statUnit("FREE", lblFree, Theme.ACCENT_GREEN));
        stats.add(statUnit("FRAGMENTATION", lblFrag, Theme.ACCENT_AMBER));
        mapCard.add(stats, BorderLayout.SOUTH);

        outer.add(mapCard, BorderLayout.CENTER);
        outer.add(buildAllocPanel(), BorderLayout.EAST);
        return outer;
    }

    private JLabel statLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_MONO_LG);
        l.setForeground(Theme.TEXT_PRIMARY);
        return l;
    }

    private JPanel statUnit(String title, JLabel valueLabel, Color col) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(Theme.BG_CARD);
        JLabel tl = UIHelper.muted(title);
        valueLabel.setForeground(col);
        p.add(tl, BorderLayout.NORTH);
        p.add(valueLabel, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildAllocPanel() {
        JPanel card = UIHelper.card();
        card.setLayout(new BorderLayout(0, Theme.GAP));
        card.setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        card.setPreferredSize(new Dimension(240, 10));

        card.add(UIHelper.heading("ALLOCATE"), BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);

        spAllocPid = UIHelper.intSpinner(1, 1, 9999);
        tfAllocName = UIHelper.textField("process name");
        spAllocSize = UIHelper.intSpinner(64, 1, 65536);
        spFreePid = UIHelper.intSpinner(1, 1, 9999);

        fields.add(UIHelper.formRow("PID", spAllocPid));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("Name", tfAllocName));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("Size", spAllocSize));
        fields.add(Box.createVerticalStrut(10));

        btnAlloc = UIHelper.primaryButton("ALLOCATE");
        btnAlloc.addActionListener(e -> allocate());
        btnAlloc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        fields.add(btnAlloc);
        fields.add(Box.createVerticalStrut(14));

        JSeparator sep = UIHelper.separator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        fields.add(sep);
        fields.add(Box.createVerticalStrut(10));
        fields.add(UIHelper.formRow("Free PID", spFreePid));
        fields.add(Box.createVerticalStrut(6));

        btnFree = UIHelper.dangerButton("FREE");
        btnFree.addActionListener(e -> freeMemory());
        btnFree.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        fields.add(btnFree);

        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBottomRow() {
        JPanel p = UIHelper.card();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));
        p.setPreferredSize(new Dimension(700, 160));

        p.add(UIHelper.heading("MEMORY BLOCK TABLE"), BorderLayout.NORTH);

        String[] cols = {"Start Addr", "End Addr", "Size", "PID", "Name", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UIHelper.styleTable(table);
        table.setRowHeight(24);

        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                String status = val == null ? "" : val.toString();
                boolean used = status.startsWith("USED");
                l.setFont(Theme.FONT_SMALL);
                l.setForeground(sel ? Theme.BG_DEEP : (used ? Theme.ACCENT_GREEN : Theme.TEXT_MUTED));
                l.setBackground(sel ? Theme.BG_SELECTED : Theme.alpha(used ? Theme.ACCENT_GREEN : Theme.BORDER_DIM, 22));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(new EmptyBorder(1, 4, 1, 4));
                l.setOpaque(true);
                return l;
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                l.setFont(Theme.FONT_SMALL);
                l.setForeground(sel ? Theme.ACCENT_CYAN : Theme.TEXT_PRIMARY);
                l.setBackground(sel ? Theme.BG_SELECTED : (row % 2 == 0 ? Theme.BG_PANEL : Theme.BG_ROW_ALT));
                l.setBorder(new EmptyBorder(1, 6, 1, 6));
                return l;
            }
        });

        p.add(UIHelper.scrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void initMemory() {
        int size = (Integer) spMemSize.getValue();
        String strat = (String) cbStrategy.getSelectedItem();
        try {
            AllocationStrategy s = AllocationStrategy.fromString(strat);
            ctx.reinitMemory(size, s);
            memMapPanel.reset();
            refresh();
            flashStatus("Memory initialised: " + size + " units, " + strat, Theme.ACCENT_GREEN);
        } catch (Exception ex) {
            flashStatus(ex.getMessage(), Theme.ACCENT_RED);
        }
    }

    private void allocate() {
        int pid = (Integer) spAllocPid.getValue();
        String name = tfAllocName.getText().trim();
        if (name.isEmpty()) name = "P" + pid;
        int size = (Integer) spAllocSize.getValue();
        int start = ctx.getMemoryManager().allocate(pid, name, size);
        if (start < 0) {
            flashStatus("Allocation failed - not enough free memory for " + size + " units", Theme.ACCENT_RED);
        } else {
            ctx.getProcessManager().findByPid(pid).ifPresent(p -> {
                p.setMemoryStart(start);
                p.setMemorySize(size);
            });
            refresh();
            flashStatus("P" + pid + " allocated " + size + " units at address " + start, Theme.ACCENT_GREEN);
        }
    }

    private void freeMemory() {
        int pid = (Integer) spFreePid.getValue();
        boolean freed = ctx.getMemoryManager().free(pid);
        ctx.getProcessManager().findByPid(pid).ifPresent(p -> {
            p.setMemoryStart(-1);
            p.setMemorySize(0);
        });
        refresh();
        flashStatus(freed ? "P" + pid + " memory freed" : "No memory for P" + pid,
                freed ? Theme.ACCENT_GREEN : Theme.ACCENT_AMBER);
    }

    public void refresh() {
        MemoryManager mm = ctx.getMemoryManager();
        List<MemoryBlock> blocks = mm.getMemoryMap();

        memMapPanel.setBlocks(blocks, mm.getTotalMemory());

        lblTotal.setText(mm.getTotalMemory() + " u");
        lblUsed.setText(mm.getUsedMemory() + " u");
        lblFree.setText(mm.getFreeMemory() + " u");
        lblFrag.setText(String.format("%.1f%%", mm.getFragmentationRatio() * 100));

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
        lblStatus.setText(msg);
        lblStatus.setForeground(col);
        javax.swing.Timer t = new javax.swing.Timer(4000, e -> {
            lblStatus.setForeground(Theme.TEXT_MUTED);
            lblStatus.setText("Initialise memory, then allocate blocks to processes.");
        });
        t.setRepeats(false);
        t.start();
    }
}
