package os.gui;

import os.scheduler.GanttEntry;
import os.scheduler.ScheduleResult;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.List;
import java.util.*;

/**
 * Custom-painted Gantt chart component.
 *
 * Renders the CPU execution timeline as coloured blocks with:
 *   - Rounded rectangles per process slice (colour keyed by PID)
 *   - IDLE blocks in dim fill
 *   - Time axis with tick marks beneath
 *   - Process name + duration label inside each block
 *   - Tooltip on hover showing start/end/duration
 *   - Legend row at bottom
 */
public class GanttChartPanel extends JPanel {

    private ScheduleResult result;
    private final Map<Integer, Integer> pidToIndex = new LinkedHashMap<>();

    // Layout constants
    private static final int BAR_H       = 44;
    private static final int AXIS_H      = 28;
    private static final int LEGEND_H    = 32;
    private static final int TOP_PAD     = 18;
    private static final int SIDE_PAD    = 16;
    private static final int MIN_CELL_W  = 48;

    // Hover tracking
    private int hoveredIndex = -1;

    public GanttChartPanel() {
        setBackground(Theme.BG_DEEP);
        setPreferredSize(new Dimension(800, BAR_H + AXIS_H + LEGEND_H + TOP_PAD + 20));
        setOpaque(true);
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                updateHover(e.getX(), e.getY());
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                hoveredIndex = -1; repaint();
            }
        });
        setToolTipText(""); // enable tooltips
    }

    public void setResult(ScheduleResult r) {
        this.result = r;
        this.pidToIndex.clear();
        if (r != null) {
            int idx = 0;
            for (GanttEntry e : r.getGanttChart()) {
                if (!e.isIdle() && !pidToIndex.containsKey(e.getPid()))
                    pidToIndex.put(e.getPid(), idx++);
            }
        }
        repaint();
    }

    @Override public String getToolTipText(java.awt.event.MouseEvent e) {
        if (result == null) return null;
        int idx = findEntryAt(e.getX());
        if (idx < 0 || idx >= result.getGanttChart().size()) return null;
        GanttEntry entry = result.getGanttChart().get(idx);
        return String.format("<html><b>%s</b><br>Start: %d &nbsp; End: %d &nbsp; Duration: %d</html>",
                entry.isIdle() ? "IDLE" : "P" + entry.getPid() + " — " + entry.getProcessName(),
                entry.getStartTime(), entry.getEndTime(), entry.getDuration());
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (result == null || result.getGanttChart().isEmpty()) {
            drawPlaceholder(g2);
            g2.dispose();
            return;
        }

        List<GanttEntry> gantt   = result.getGanttChart();
        int              total   = result.getTotalTime();
        int              drawW   = getWidth() - 2 * SIDE_PAD;
        float            scale   = (float) drawW / Math.max(total, 1);
        float            cellMin = MIN_CELL_W;

        // If total time would make cells too narrow, expand the panel
        float neededW = Math.max(drawW, gantt.size() * (int) cellMin);
        if (neededW > drawW) {
            scale = neededW / Math.max(total, 1);
            setPreferredSize(new Dimension((int)(neededW + 2*SIDE_PAD),
                    BAR_H + AXIS_H + LEGEND_H + TOP_PAD + 20));
            revalidate();
        }

        int barY = TOP_PAD;

        for (int i = 0; i < gantt.size(); i++) {
            GanttEntry e    = gantt.get(i);
            int        x    = SIDE_PAD + (int)(e.getStartTime() * scale);
            int        w    = Math.max(2, (int)(e.getDuration() * scale));
            boolean    idle = e.isIdle();
            boolean    hov  = (i == hoveredIndex);

            Color fill = idle ? Theme.GANTT_IDLE
                    : Theme.alpha(Theme.ganttColour(pidToIndex.getOrDefault(e.getPid(), 0)),
                                  hov ? 220 : 160);
            Color border = idle ? Theme.BORDER_NORMAL
                    : Theme.ganttColour(pidToIndex.getOrDefault(e.getPid(), 0));

            // Block fill
            g2.setColor(fill);
            g2.fillRoundRect(x, barY, w, BAR_H, 6, 6);

            // Border
            g2.setColor(hov ? Theme.lighter(border, 40) : border);
            g2.setStroke(new BasicStroke(hov ? 2f : 1f));
            g2.drawRoundRect(x, barY, w-1, BAR_H-1, 6, 6);

            // Top glow line
            if (!idle) {
                g2.setColor(Theme.alpha(border, hov ? 255 : 180));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(x+3, barY+1, x+w-4, barY+1);
            }

            // Label inside block (only if wide enough)
            if (w > 24) {
                String lbl = idle ? "IDLE" : "P" + e.getPid();
                g2.setFont(w > 60 ? Theme.FONT_SUBHEAD : Theme.FONT_SMALL);
                FontMetrics fm = g2.getFontMetrics();
                int lx = x + (w - fm.stringWidth(lbl)) / 2;
                int ly = barY + (BAR_H + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(idle ? Theme.TEXT_MUTED : Theme.BG_DEEP);
                g2.drawString(lbl, lx, ly);
                // Duration sub-label
                if (w > 60 && !idle) {
                    String dur = "+" + e.getDuration();
                    g2.setFont(Theme.FONT_SMALL);
                    fm = g2.getFontMetrics();
                    g2.setColor(Theme.alpha(Theme.BG_DEEP, 160));
                    g2.drawString(dur, x + (w - fm.stringWidth(dur))/2, ly + 13);
                }
            }
        }

        // ── Time axis ───────────────────────────────────────────────────────
        int axisY = barY + BAR_H + 4;
        g2.setColor(Theme.BORDER_NORMAL);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(SIDE_PAD, axisY, SIDE_PAD + (int)(total * scale), axisY);

        Set<Integer> drawn = new HashSet<>();
        for (GanttEntry e : gantt) {
            for (int t : new int[]{e.getStartTime(), e.getEndTime()}) {
                if (drawn.add(t)) {
                    int tx = SIDE_PAD + (int)(t * scale);
                    g2.setColor(Theme.BORDER_NORMAL);
                    g2.drawLine(tx, axisY, tx, axisY + 5);
                    g2.setFont(Theme.FONT_SMALL);
                    g2.setColor(Theme.TEXT_SECONDARY);
                    String ts = String.valueOf(t);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(ts, tx - fm.stringWidth(ts)/2, axisY + 18);
                }
            }
        }

        // ── Legend row ───────────────────────────────────────────────────────
        int legY = axisY + AXIS_H + 4;
        int legX = SIDE_PAD;
        g2.setFont(Theme.FONT_SMALL);
        FontMetrics lfm = g2.getFontMetrics();

        for (Map.Entry<Integer, Integer> entry : pidToIndex.entrySet()) {
            int pid = entry.getKey(); int idx2 = entry.getValue();
            // Find name
            String name = result.getCompletedProcesses().stream()
                    .filter(p -> p.getPid() == pid)
                    .map(p -> "P" + pid + ":" + p.getName())
                    .findFirst().orElse("P" + pid);

            Color col = Theme.ganttColour(idx2);
            g2.setColor(col);
            g2.fillRoundRect(legX, legY + 4, 12, 12, 3, 3);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(name, legX + 16, legY + 14);
            legX += lfm.stringWidth(name) + 28;
            if (legX > getWidth() - 80) break;
        }

        g2.dispose();
    }

    private void drawPlaceholder(Graphics2D g2) {
        g2.setFont(Theme.FONT_BODY);
        g2.setColor(Theme.TEXT_MUTED);
        String msg = "Run a simulation to see the Gantt chart";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (getWidth()-fm.stringWidth(msg))/2, getHeight()/2);
        // Dashed border
        g2.setColor(Theme.BORDER_DIM);
        float[] dash = {6f, 4f};
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
        g2.drawRoundRect(SIDE_PAD, TOP_PAD, getWidth()-2*SIDE_PAD, BAR_H, 6, 6);
    }

    private void updateHover(int mx, int my) {
        int newHover = findEntryAt(mx);
        if (newHover != hoveredIndex) { hoveredIndex = newHover; repaint(); }
    }

    private int findEntryAt(int mx) {
        if (result == null) return -1;
        List<GanttEntry> gantt = result.getGanttChart();
        int total = result.getTotalTime();
        int drawW = getWidth() - 2 * SIDE_PAD;
        float scale = (float) Math.max(drawW, gantt.size() * MIN_CELL_W) / Math.max(total, 1);
        for (int i = 0; i < gantt.size(); i++) {
            GanttEntry e = gantt.get(i);
            int x = SIDE_PAD + (int)(e.getStartTime() * scale);
            int w = Math.max(2, (int)(e.getDuration() * scale));
            if (mx >= x && mx <= x + w) return i;
        }
        return -1;
    }
}
