package os.gui;

import os.cli.SimulationContext;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Main application window for the Mini OS Simulator  v0.7 — Phase 2 GUI.
 *
 * Layout:
 *   ┌─────────────────────────────────────────────────────┐
 *   │  HEADER BAR (logo + subtitle)                        │
 *   ├──────────┬──────────────────────────────────────────┤
 *   │          │                                          │
 *   │ SIDEBAR  │     CONTENT  (CardLayout)                │
 *   │  nav     │                                          │
 *   │  buttons │                                          │
 *   │          │                                          │
 *   ├──────────┴──────────────────────────────────────────┤
 *   │  STATUS BAR (process count, scheduler, memory)       │
 *   └─────────────────────────────────────────────────────┘
 */
public class MainWindow extends JFrame {

    private final SimulationContext ctx = new SimulationContext();

    // Panels
    private ProcessPanel   processPanel;
    private SchedulerPanel schedulerPanel;
    private MemoryPanel    memoryPanel;
    private FileSystemPanel fsPanel;

    // Navigation
    private JPanel     contentPanel;
    private CardLayout cardLayout;
    private JButton    activeNavBtn = null;

    // Status bar labels
    private JLabel lblProcCount, lblScheduler, lblMemory, lblCwd;

    // Nav section data
    private static final String[][] NAV_ITEMS = {
        {"PROCESSES",  "⬡", "process",   "Manage PCBs: create, list, remove"},
        {"SCHEDULING", "▶", "scheduler", "Run CPU algorithms & view Gantt charts"},
        {"MEMORY",     "▦", "memory",    "Allocate & visualise physical memory"},
        {"FILESYSTEM", "⬢", "fs",        "Navigate the virtual file system"},
    };

    public MainWindow() {
        super("Mini OS Simulator  v0.7 — Phase 2");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(900, 620));

        // Apply global Swing overrides for dark theme
        applyGlobalTheme();

        buildUI();
        setLocationRelativeTo(null);
        setVisible(true);

        // Auto-refresh status every second
        new javax.swing.Timer(1000, e -> updateStatusBar()).start();
    }

    // ── Global theme ──────────────────────────────────────────────────────────

    private void applyGlobalTheme() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}

        Color[] darkBg = { Theme.BG_PANEL, Theme.BG_CARD, Theme.BG_INPUT };
        String[] bgKeys = {
            "Panel.background","ScrollPane.background","Viewport.background",
            "SplitPane.background","TabbedPane.background"
        };
        for (String k : bgKeys) UIManager.put(k, Theme.BG_PANEL);
        UIManager.put("SplitPaneDivider.draggingColor", Theme.BORDER_NORMAL);
        UIManager.put("SplitPane.dividerSize", 4);
        UIManager.put("Table.background",          Theme.BG_PANEL);
        UIManager.put("Table.foreground",          Theme.TEXT_PRIMARY);
        UIManager.put("Table.gridColor",           Theme.BORDER_DIM);
        UIManager.put("TableHeader.background",    Theme.BG_HEADER);
        UIManager.put("TableHeader.foreground",    Theme.ACCENT_TEAL);
        UIManager.put("ComboBox.background",       Theme.BG_INPUT);
        UIManager.put("ComboBox.foreground",       Theme.TEXT_PRIMARY);
        UIManager.put("TextField.background",      Theme.BG_INPUT);
        UIManager.put("TextField.foreground",      Theme.TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", Theme.ACCENT_CYAN);
        UIManager.put("Spinner.background",        Theme.BG_INPUT);
        UIManager.put("CheckBox.background",       Theme.BG_CARD);
        UIManager.put("CheckBox.foreground",       Theme.TEXT_SECONDARY);
        UIManager.put("RadioButton.background",    Theme.BG_CARD);
        UIManager.put("RadioButton.foreground",    Theme.TEXT_SECONDARY);
        UIManager.put("ToolTip.background",        Theme.BG_CARD);
        UIManager.put("ToolTip.foreground",        Theme.TEXT_PRIMARY);
        UIManager.put("ToolTip.border",            BorderFactory.createLineBorder(Theme.BORDER_NORMAL));
        UIManager.put("Tree.background",           Theme.BG_CARD);
        UIManager.put("Tree.foreground",           Theme.TEXT_PRIMARY);
    }

    // ── Build layout ──────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Theme.BG_DEEP);

        root.add(buildHeader(),    BorderLayout.NORTH);
        root.add(buildSidebar(),   BorderLayout.WEST);
        root.add(buildContent(),   BorderLayout.CENTER);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradient header
                GradientPaint gp = new GradientPaint(0,0, Theme.BG_HEADER,
                        getWidth(), 0, new Color(0x0D, 0x1A, 0x2A));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Bottom accent line
                g2.setColor(Theme.ACCENT_CYAN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, Theme.HEADER_H));
        header.setBorder(new EmptyBorder(0, Theme.PADDING, 0, Theme.PADDING));

        // Logo area
        JPanel logoArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        logoArea.setOpaque(false);

        // Hexagon logo
        JPanel hexLogo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth()/2, cy = getHeight()/2, r = 18;
                int[] xs = new int[6], ys = new int[6];
                for (int i = 0; i < 6; i++) {
                    xs[i] = (int)(cx + r * Math.cos(Math.toRadians(60*i - 30)));
                    ys[i] = (int)(cy + r * Math.sin(Math.toRadians(60*i - 30)));
                }
                g2.setColor(Theme.alpha(Theme.ACCENT_CYAN, 30));
                g2.fillPolygon(xs, ys, 6);
                g2.setColor(Theme.ACCENT_CYAN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawPolygon(xs, ys, 6);
                // OS text inside
                g2.setFont(new Font("Courier New", Font.BOLD, 11));
                g2.setColor(Theme.ACCENT_CYAN);
                g2.drawString("OS", cx-10, cy+4);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(44, Theme.HEADER_H); }
        };
        hexLogo.setOpaque(false);

        JLabel title = new JLabel("MINI OS SIMULATOR");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JLabel sub = new JLabel("  ·  Process  ·  Scheduling  ·  Memory  ·  Filesystem");
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_MUTED);

        logoArea.add(hexLogo);
        logoArea.add(title);
        logoArea.add(sub);

        // Right: quick stats
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        lblProcCount = headerStat("0 PROCESSES");
        lblScheduler = headerStat("FCFS");
        lblMemory    = headerStat("0 / 1024 u");
        right.add(lblProcCount);
        right.add(headerDot());
        right.add(lblScheduler);
        right.add(headerDot());
        right.add(lblMemory);

        header.add(logoArea, BorderLayout.WEST);
        header.add(right,    BorderLayout.EAST);
        return header;
    }

    private JLabel headerStat(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_SECONDARY);
        return l;
    }

    private JLabel headerDot() {
        JLabel l = new JLabel("·");
        l.setFont(Theme.FONT_BODY);
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BG_SIDEBAR);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Right border
                g2.setColor(Theme.BORDER_DIM);
                g2.drawLine(getWidth()-1, 0, getWidth()-1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(Theme.SIDEBAR_W, 0));
        sidebar.setBorder(new EmptyBorder(16, 0, 16, 0));

        // Section label
        JLabel sectionLbl = new JLabel("  MODULES");
        sectionLbl.setFont(new Font("Courier New", Font.BOLD, 9));
        sectionLbl.setForeground(Theme.TEXT_MUTED);
        sectionLbl.setBorder(new EmptyBorder(0, 14, 10, 0));
        sidebar.add(sectionLbl);

        for (String[] nav : NAV_ITEMS) {
            JButton btn = buildNavButton(nav[0], nav[1], nav[2], nav[3]);
            sidebar.add(btn);
            sidebar.add(Box.createVerticalStrut(4));
            // Auto-select first
            if (activeNavBtn == null) {
                activeNavBtn = btn;
                btn.putClientProperty("active", true);
            }
        }

        sidebar.add(Box.createVerticalGlue());

        // Bottom info
        JLabel version = new JLabel("  v1.0  Java Swing");
        version.setFont(new Font("Courier New", Font.PLAIN, 9));
        version.setForeground(Theme.TEXT_MUTED);
        version.setBorder(new EmptyBorder(8, 14, 0, 0));
        sidebar.add(version);

        return sidebar;
    }

    private JButton buildNavButton(String label, String icon, String card, String tooltip) {
        JButton btn = new JButton() {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                boolean active = Boolean.TRUE.equals(getClientProperty("active"));
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (active) {
                    // Active: left accent bar + background
                    g2.setColor(Theme.alpha(Theme.ACCENT_CYAN, 18));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(Theme.ACCENT_CYAN);
                    g2.fillRect(0, 0, 3, getHeight());
                } else if (hovered) {
                    g2.setColor(Theme.alpha(Theme.ACCENT_CYAN, 10));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                // Icon circle
                Color iconCol = active ? Theme.ACCENT_CYAN : (hovered ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);
                g2.setColor(Theme.alpha(iconCol, active ? 30 : 18));
                g2.fillOval(14, getHeight()/2-12, 24, 24);
                g2.setColor(iconCol);
                g2.setFont(new Font("Courier New", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(icon, 14 + (24-fm.stringWidth(icon))/2, getHeight()/2+5);

                // Label
                g2.setColor(active ? Theme.TEXT_PRIMARY : (hovered ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED));
                g2.setFont(active ? Theme.FONT_NAV : Theme.FONT_BODY);
                g2.drawString(label, 46, getHeight()/2+5);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        btn.setPreferredSize(new Dimension(Theme.SIDEBAR_W, 46));
        btn.setMaximumSize (new Dimension(Theme.SIDEBAR_W, 46));
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tooltip);
        btn.addActionListener(e -> {
            if (activeNavBtn != null) { activeNavBtn.putClientProperty("active", false); activeNavBtn.repaint(); }
            btn.putClientProperty("active", true);
            activeNavBtn = btn;
            cardLayout.show(contentPanel, card);
            btn.repaint();
        });
        return btn;
    }

    // ── Content area ──────────────────────────────────────────────────────────

    private JPanel buildContent() {
        processPanel   = new ProcessPanel(ctx);
        schedulerPanel = new SchedulerPanel(ctx);
        memoryPanel    = new MemoryPanel(ctx);
        fsPanel        = new FileSystemPanel(ctx);

        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Theme.BG_PANEL);
        contentPanel.add(processPanel,   "process");
        contentPanel.add(schedulerPanel, "scheduler");
        contentPanel.add(memoryPanel,    "memory");
        contentPanel.add(fsPanel,        "fs");

        cardLayout.show(contentPanel, "process");
        return contentPanel;
    }

    // ── Status bar ────────────────────────────────────────────────────────────

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.BG_HEADER);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BORDER_DIM);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(0, Theme.STATUS_H));
        bar.setBorder(new EmptyBorder(0, Theme.PADDING, 0, 0));
        bar.setOpaque(false);

        // Pulse dot
        JLabel dot = new JLabel("●");
        dot.setFont(Theme.FONT_SMALL);
        dot.setForeground(Theme.ACCENT_GREEN);

        lblCwd = statusLabel("/ ");

        bar.add(dot);
        bar.add(statusLabel("Mini OS Simulator  v0.7 — Phase 2"));
        bar.add(statusSep());
        bar.add(statusLabel("Processes: "));
        lblProcCount = statusLabel("0");
        bar.add(lblProcCount);
        bar.add(statusSep());
        bar.add(statusLabel("Scheduler: "));
        lblScheduler = statusLabel("FCFS");
        bar.add(lblScheduler);
        bar.add(statusSep());
        bar.add(statusLabel("Memory: "));
        lblMemory = statusLabel("0/1024 u");
        bar.add(lblMemory);
        bar.add(statusSep());
        bar.add(statusLabel("cwd: "));
        bar.add(lblCwd);

        return bar;
    }

    private JLabel statusLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_SECONDARY);
        return l;
    }

    private JLabel statusSep() {
        JLabel l = new JLabel("│");
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    // ── Status refresh ────────────────────────────────────────────────────────

    private void updateStatusBar() {
        int procs = ctx.getProcessManager().size();
        lblProcCount.setText(String.valueOf(procs));
        lblScheduler.setText(ctx.getActiveScheduler().getName()
                .replaceFirst(" \\(.*\\)$","").trim());
        lblMemory.setText(ctx.getMemoryManager().getUsedMemory() + " / "
                + ctx.getMemoryManager().getTotalMemory() + " u");
        lblCwd.setText(ctx.getFileSystem().pwd());
    }
}
