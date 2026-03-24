package os.gui;

import os.memory.MemoryBlock;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Custom-painted memory map visualiser.
 *
 * Renders the physical address space as a horizontal segmented bar.
 * Each allocated block is filled with a process-specific colour; free
 * blocks are rendered with a dark hatched pattern.  Hovering a block
 * shows a tooltip with address range, size, and PID.
 */
public class MemoryMapPanel extends JPanel {

    private List<MemoryBlock> blocks = new ArrayList<>();
    private int               totalMemory = 1024;
    private int               hoveredIndex = -1;

    // Colour cycling per PID
    private final Map<Integer, Color> pidColors = new LinkedHashMap<>();
    private int colorCursor = 0;

    private static final int BAR_H    = 54;
    private static final int AXIS_H   = 22;
    private static final int TOP_PAD  = 12;
    private static final int SIDE_PAD = 16;

    public MemoryMapPanel() {
        setBackground(Theme.BG_DEEP);
        setOpaque(true);
        setPreferredSize(new Dimension(600, BAR_H + AXIS_H + TOP_PAD + 20));
        setToolTipText("");
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int newHover = findBlockAt(e.getX());
                if (newHover != hoveredIndex) { hoveredIndex = newHover; repaint(); }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hoveredIndex = -1; repaint(); }
        });
    }

    public void setBlocks(List<MemoryBlock> blocks, int totalMemory) {
        this.blocks      = blocks == null ? new ArrayList<>() : blocks;
        this.totalMemory = Math.max(totalMemory, 1);
        // Assign colours to new PIDs
        for (MemoryBlock b : this.blocks) {
            if (b.isAllocated() && !pidColors.containsKey(b.getPid())) {
                pidColors.put(b.getPid(), Theme.ganttColour(colorCursor++));
            }
        }
        repaint();
    }

    public void reset() {
        blocks.clear(); pidColors.clear(); colorCursor = 0; repaint();
    }

    @Override public String getToolTipText(MouseEvent e) {
        int idx = findBlockAt(e.getX());
        if (idx < 0 || idx >= blocks.size()) return null;
        MemoryBlock b = blocks.get(idx);
        if (b.isAllocated())
            return String.format("<html><b>P%d — %s</b><br>Start: %d &nbsp; End: %d &nbsp; Size: %d units</html>",
                    b.getPid(), b.getProcessName(), b.getStartAddress(), b.getEndAddress(), b.getSize());
        return String.format("<html><b>FREE</b><br>Start: %d &nbsp; End: %d &nbsp; Size: %d units</html>",
                b.getStartAddress(), b.getEndAddress(), b.getSize());
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (blocks.isEmpty()) { drawPlaceholder(g2); g2.dispose(); return; }

        int drawW = getWidth() - 2 * SIDE_PAD;
        float scale = (float) drawW / totalMemory;
        int barY   = TOP_PAD;

        for (int i = 0; i < blocks.size(); i++) {
            MemoryBlock b   = blocks.get(i);
            int         x   = SIDE_PAD + (int)(b.getStartAddress() * scale);
            int         w   = Math.max(2, (int)(b.getSize() * scale));
            boolean     hov = (i == hoveredIndex);

            if (b.isAllocated()) {
                Color col = pidColors.getOrDefault(b.getPid(), Theme.ACCENT_CYAN);
                // Gradient fill
                GradientPaint gp = new GradientPaint(
                        x, barY,       Theme.lighter(col, hov ? 50 : 20),
                        x, barY+BAR_H, Theme.alpha(col, hov ? 200 : 140));
                g2.setPaint(gp);
                g2.fillRoundRect(x+1, barY, w-2, BAR_H, 4, 4);
                // Border glow
                g2.setColor(hov ? Theme.lighter(col, 60) : col);
                g2.setStroke(new BasicStroke(hov ? 2f : 1.2f));
                g2.drawRoundRect(x+1, barY, w-2, BAR_H-1, 4, 4);
                // Top glow
                g2.setColor(Theme.alpha(col, 200));
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(x+2, barY+1, x+w-3, barY+1);
                // Label
                if (w > 20) {
                    String lbl = "P" + b.getPid();
                    g2.setFont(w > 50 ? Theme.FONT_SUBHEAD : Theme.FONT_SMALL);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.setColor(Theme.BG_DEEP);
                    if (fm.stringWidth(lbl) < w - 4)
                        g2.drawString(lbl, x + (w - fm.stringWidth(lbl))/2,
                                barY + (BAR_H + fm.getAscent())/2 - 2);
                }
            } else {
                // Free block — dark hatch
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(x+1, barY, w-2, BAR_H, 4, 4);
                g2.setColor(Theme.alpha(Theme.BORDER_NORMAL, hov ? 200 : 100));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x+1, barY, w-2, BAR_H-1, 4, 4);
                // Hatch lines
                g2.setColor(Theme.alpha(Theme.BORDER_DIM, 140));
                g2.setStroke(new BasicStroke(1f));
                Shape clip = new RoundRect(x+1, barY, w-2, BAR_H, 4);
                g2.setClip(clip);
                for (int hx = x-BAR_H; hx < x+w; hx += 10)
                    g2.drawLine(hx, barY, hx+BAR_H, barY+BAR_H);
                g2.setClip(null);
                if (w > 30) {
                    g2.setFont(Theme.FONT_SMALL);
                    FontMetrics fm = g2.getFontMetrics();
                    String lbl = "FREE";
                    if (fm.stringWidth(lbl) < w-4) {
                        g2.setColor(Theme.TEXT_MUTED);
                        g2.drawString(lbl, x+(w-fm.stringWidth(lbl))/2,
                                barY+(BAR_H+fm.getAscent())/2-2);
                    }
                }
            }
        }

        // ── Address axis ─────────────────────────────────────────────────────
        int axisY = barY + BAR_H + 4;
        g2.setColor(Theme.BORDER_NORMAL);
        g2.setStroke(new BasicStroke(1f));
        g2.setClip(null);
        g2.drawLine(SIDE_PAD, axisY, SIDE_PAD + drawW, axisY);

        Set<Integer> drawnAddrs = new HashSet<>();
        for (MemoryBlock b : blocks) {
            for (int addr : new int[]{b.getStartAddress(), b.getEndAddress()+1}) {
                if (addr > totalMemory) continue;
                if (drawnAddrs.add(addr)) {
                    int tx = SIDE_PAD + (int)(addr * scale);
                    g2.setColor(Theme.BORDER_NORMAL);
                    g2.drawLine(tx, axisY, tx, axisY+4);
                    String lbl = String.valueOf(addr);
                    g2.setFont(Theme.FONT_SMALL);
                    FontMetrics fm = g2.getFontMetrics();
                    if (drawnAddrs.size() < 20 || addr == 0 || addr == totalMemory) {
                        g2.setColor(Theme.TEXT_SECONDARY);
                        g2.drawString(lbl, tx - fm.stringWidth(lbl)/2, axisY+16);
                    }
                }
            }
        }

        // ── Legend ────────────────────────────────────────────────────────────
        int legX = SIDE_PAD, legY = axisY + AXIS_H + 2;
        g2.setFont(Theme.FONT_SMALL);
        FontMetrics lfm = g2.getFontMetrics();

        for (Map.Entry<Integer, Color> en : pidColors.entrySet()) {
            if (legX > getWidth() - 80) break;
            // Find process name from blocks
            String name = blocks.stream()
                    .filter(b -> b.isAllocated() && b.getPid() == en.getKey())
                    .map(b -> "P" + b.getPid() + ":" + b.getProcessName())
                    .findFirst().orElse("P" + en.getKey());
            g2.setColor(en.getValue());
            g2.fillRoundRect(legX, legY+2, 10, 10, 2, 2);
            g2.setColor(Theme.TEXT_SECONDARY);
            g2.drawString(name, legX+14, legY+11);
            legX += lfm.stringWidth(name) + 26;
        }

        g2.dispose();
    }

    private void drawPlaceholder(Graphics2D g2) {
        g2.setFont(Theme.FONT_BODY);
        g2.setColor(Theme.TEXT_MUTED);
        String msg = "Allocate memory to see the memory map";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (getWidth()-fm.stringWidth(msg))/2, getHeight()/2);
        float[] dash = {6,4};
        g2.setStroke(new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10,dash,0));
        g2.setColor(Theme.BORDER_DIM);
        g2.drawRoundRect(SIDE_PAD, TOP_PAD, getWidth()-2*SIDE_PAD, BAR_H, 6, 6);
    }

    private int findBlockAt(int mx) {
        if (blocks.isEmpty()) return -1;
        int drawW = getWidth() - 2*SIDE_PAD;
        float scale = (float)drawW / totalMemory;
        for (int i = 0; i < blocks.size(); i++) {
            MemoryBlock b = blocks.get(i);
            int x = SIDE_PAD + (int)(b.getStartAddress() * scale);
            int w = Math.max(2, (int)(b.getSize() * scale));
            if (mx >= x && mx <= x+w) return i;
        }
        return -1;
    }

    // Helper shape for clipping
    private static class RoundRect extends java.awt.geom.RoundRectangle2D.Float {
        RoundRect(float x, float y, float w, float h, float r) { super(x,y,w,h,r,r); }
    }
}
