package os.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Factory and utility class for creating consistently styled Swing components.
 * All UI construction goes through here to guarantee theme coherence.
 */
public final class UIHelper {

    private UIHelper() {}

    // ── Labels ────────────────────────────────────────────────────────────────

    public static JLabel label(String text, Font font, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(fg);
        l.setOpaque(false);
        return l;
    }

    public static JLabel heading(String text) {
        return label(text, Theme.FONT_HEADING, Theme.ACCENT_CYAN);
    }

    public static JLabel subheading(String text) {
        return label(text, Theme.FONT_SUBHEAD, Theme.TEXT_PRIMARY);
    }

    public static JLabel body(String text) {
        return label(text, Theme.FONT_BODY, Theme.TEXT_SECONDARY);
    }

    public static JLabel muted(String text) {
        return label(text, Theme.FONT_SMALL, Theme.TEXT_MUTED);
    }

    public static JLabel badge(String text, Color bg) {
        JLabel l = new JLabel(" " + text + " ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.alpha(bg, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.setColor(bg);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(bg);
        l.setOpaque(false);
        return l;
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    /** Primary action button with neon-cyan border glow. */
    public static JButton primaryButton(String text) {
        return styledButton(text, Theme.ACCENT_CYAN, Theme.BG_CARD);
    }

    /** Secondary/danger button. */
    public static JButton dangerButton(String text) {
        return styledButton(text, Theme.ACCENT_RED, Theme.BG_CARD);
    }

    /** Ghost/neutral button. */
    public static JButton ghostButton(String text) {
        return styledButton(text, Theme.TEXT_SECONDARY, Theme.BG_CARD);
    }

    public static JButton successButton(String text) {
        return styledButton(text, Theme.ACCENT_GREEN, Theme.BG_CARD);
    }

    private static JButton styledButton(String text, Color accent, Color bg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = hovered ? Theme.alpha(accent, 35) : Theme.alpha(accent, 18);
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.CORNER_RADIUS, Theme.CORNER_RADIUS);
                g2.setColor(hovered ? accent : Theme.alpha(accent, 140));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1,
                        Theme.CORNER_RADIUS, Theme.CORNER_RADIUS);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override protected void paintBorder(Graphics g) {}
        };
        btn.setFont(Theme.FONT_SUBHEAD);
        btn.setForeground(accent);
        btn.setBackground(bg);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16,
                                           btn.getPreferredSize().height + 6));
        return btn;
    }

    // ── Text Fields ───────────────────────────────────────────────────────────

    public static JTextField textField(String placeholder) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.setColor(hasFocus() ? Theme.ACCENT_CYAN : Theme.BORDER_NORMAL);
                g2.setStroke(new BasicStroke(hasFocus() ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        f.setFont(Theme.FONT_CODE);
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setBackground(Theme.BG_INPUT);
        f.setCaretColor(Theme.ACCENT_CYAN);
        f.setBorder(new EmptyBorder(6, 10, 6, 10));
        f.setOpaque(false);
        if (!placeholder.isEmpty()) {
            f.putClientProperty("placeholder", placeholder);
            f.addFocusListener(new FocusAdapter() {
                public void focusGained(FocusEvent e) { f.repaint(); }
                public void focusLost (FocusEvent e)  { f.repaint(); }
            });
        }
        return f;
    }

    public static JTextField textField() { return textField(""); }

    // ── Spinners ──────────────────────────────────────────────────────────────

    public static JSpinner intSpinner(int val, int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(val, min, max, 1));
        s.setFont(Theme.FONT_CODE);
        s.setBackground(Theme.BG_INPUT);
        s.setForeground(Theme.TEXT_PRIMARY);
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(Theme.BG_INPUT);
            de.getTextField().setForeground(Theme.TEXT_PRIMARY);
            de.getTextField().setFont(Theme.FONT_CODE);
            de.getTextField().setCaretColor(Theme.ACCENT_CYAN);
            de.getTextField().setBorder(new EmptyBorder(4, 6, 4, 6));
        }
        s.setBorder(BorderFactory.createLineBorder(Theme.BORDER_NORMAL, 1));
        return s;
    }

    // ── Combo Boxes ───────────────────────────────────────────────────────────

    public static <T> JComboBox<T> comboBox(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setFont(Theme.FONT_CODE);
        cb.setBackground(Theme.BG_INPUT);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,
                    Object val, int idx, boolean sel, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list,val,idx,sel,focus);
                l.setFont(Theme.FONT_CODE);
                l.setForeground(sel ? Theme.BG_DEEP : Theme.TEXT_PRIMARY);
                l.setBackground(sel ? Theme.ACCENT_CYAN : Theme.BG_INPUT);
                l.setBorder(new EmptyBorder(4,8,4,8));
                return l;
            }
        });
        cb.setBorder(BorderFactory.createLineBorder(Theme.BORDER_NORMAL));
        return cb;
    }

    // ── Tables ────────────────────────────────────────────────────────────────

    public static void styleTable(JTable table) {
        table.setBackground(Theme.BG_PANEL);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setFont(Theme.FONT_CODE);
        table.setGridColor(Theme.BORDER_DIM);
        table.setRowHeight(28);
        table.setSelectionBackground(Theme.BG_SELECTED);
        table.setSelectionForeground(Theme.ACCENT_CYAN);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setOpaque(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(Theme.FONT_SUBHEAD);
        header.setBackground(Theme.BG_HEADER);
        header.setForeground(Theme.ACCENT_TEAL);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.ACCENT_CYAN));
        header.setReorderingAllowed(false);
    }

    // ── Scroll Panes ─────────────────────────────────────────────────────────

    public static JScrollPane scrollPane(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBackground(Theme.BG_PANEL);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER_NORMAL, 1));
        sp.getViewport().setBackground(Theme.BG_PANEL);
        styleScrollBar(sp.getVerticalScrollBar());
        styleScrollBar(sp.getHorizontalScrollBar());
        return sp;
    }

    private static void styleScrollBar(JScrollBar bar) {
        bar.setBackground(Theme.BG_PANEL);
        bar.setForeground(Theme.BORDER_NORMAL);
        bar.setPreferredSize(new Dimension(8, 8));
        bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor   = Theme.BORDER_NORMAL;
                trackColor   = Theme.BG_PANEL;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
            }
        });
    }

    // ── Panels ───────────────────────────────────────────────────────────────

    /** Dark card panel with rounded border glow. */
    public static JPanel card() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1,
                        Theme.CORNER_RADIUS, Theme.CORNER_RADIUS);
                g2.setColor(Theme.BORDER_NORMAL);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1,
                        Theme.CORNER_RADIUS, Theme.CORNER_RADIUS);
                g2.dispose();
            }
        };
    }

    /** Flat panel with solid bg. */
    public static JPanel panel(Color bg) {
        JPanel p = new JPanel();
        p.setBackground(bg);
        p.setOpaque(true);
        return p;
    }

    // ── Dividers ─────────────────────────────────────────────────────────────

    public static JSeparator separator() {
        JSeparator s = new JSeparator();
        s.setForeground(Theme.BORDER_DIM);
        s.setBackground(Theme.BG_PANEL);
        return s;
    }

    // ── Section header with left accent bar ──────────────────────────────────

    public static JPanel sectionHeader(String title) {
        JPanel p = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.ACCENT_CYAN);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        p.setBackground(Theme.BG_PANEL);
        p.setBorder(new EmptyBorder(4, 10, 4, 0));
        JLabel lbl = label(title, Theme.FONT_HEADING, Theme.TEXT_PRIMARY);
        p.add(lbl, BorderLayout.CENTER);
        return p;
    }

    // ── Text area (console) ───────────────────────────────────────────────────

    public static JTextArea consoleArea() {
        JTextArea ta = new JTextArea();
        ta.setFont(Theme.FONT_CODE);
        ta.setBackground(Theme.BG_DEEP);
        ta.setForeground(Theme.TEXT_CODE);
        ta.setCaretColor(Theme.ACCENT_GREEN);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(false);
        ta.setBorder(new EmptyBorder(10, 12, 10, 12));
        ta.setSelectionColor(Theme.BG_SELECTED);
        ta.setSelectedTextColor(Theme.ACCENT_CYAN);
        return ta;
    }

    // ── Form row helper ───────────────────────────────────────────────────────

    public static JPanel formRow(String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel lbl = label(labelText, Theme.FONT_SMALL, Theme.TEXT_SECONDARY);
        lbl.setPreferredSize(new Dimension(90, 28));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }
}
