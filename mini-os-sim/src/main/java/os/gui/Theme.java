package os.gui;

import java.awt.*;

/**
 * Centralised design token registry for the Mini OS Simulator GUI.
 *
 * Aesthetic: "cyberpunk terminal" — deep-space dark backgrounds, vivid
 * neon-cyan/green accents, monospaced data readouts, sharp geometry.
 * Every colour, font, and dimension lives here so the whole UI stays
 * perfectly cohesive.
 */
public final class Theme {

    private Theme() {}

    // ── Palette ───────────────────────────────────────────────────────────────
    public static final Color BG_DEEP     = new Color(0x0D, 0x11, 0x17);  // near-black
    public static final Color BG_PANEL    = new Color(0x12, 0x18, 0x21);  // panel bg
    public static final Color BG_CARD     = new Color(0x1A, 0x23, 0x33);  // card/surface
    public static final Color BG_INPUT    = new Color(0x0E, 0x16, 0x22);  // input fields
    public static final Color BG_SIDEBAR  = new Color(0x09, 0x0E, 0x17);  // sidebar
    public static final Color BG_HEADER   = new Color(0x10, 0x15, 0x20);  // header strip
    public static final Color BG_ROW_ALT  = new Color(0x14, 0x1E, 0x2E);  // table alt row
    public static final Color BG_HOVER    = new Color(0x1E, 0x2D, 0x44);  // hover
    public static final Color BG_SELECTED = new Color(0x0A, 0x3D, 0x62);  // selected

    // Neon accents
    public static final Color ACCENT_CYAN   = new Color(0x00, 0xE5, 0xFF); // primary neon cyan
    public static final Color ACCENT_GREEN  = new Color(0x00, 0xFF, 0x88); // success/running
    public static final Color ACCENT_AMBER  = new Color(0xFF, 0xBF, 0x00); // warning/waiting
    public static final Color ACCENT_RED    = new Color(0xFF, 0x3D, 0x60); // error/terminated
    public static final Color ACCENT_PURPLE = new Color(0xBD, 0x93, 0xF9); // ready state
    public static final Color ACCENT_BLUE   = new Color(0x29, 0x8E, 0xFF); // new state
    public static final Color ACCENT_TEAL   = new Color(0x1A, 0xE8, 0xC8); // headers

    // Text hierarchy
    public static final Color TEXT_PRIMARY   = new Color(0xE0, 0xF0, 0xFF); // main text
    public static final Color TEXT_SECONDARY = new Color(0x7A, 0x9B, 0xC0); // secondary
    public static final Color TEXT_MUTED     = new Color(0x3E, 0x5A, 0x78); // muted/disabled
    public static final Color TEXT_CODE      = new Color(0x7D, 0xE8, 0xB5); // mono/code values

    // Borders & dividers
    public static final Color BORDER_DIM    = new Color(0x1C, 0x2C, 0x3E);
    public static final Color BORDER_NORMAL = new Color(0x25, 0x3D, 0x56);
    public static final Color BORDER_BRIGHT = new Color(0x00, 0xE5, 0xFF, 80);

    // Gantt process colours (cycling palette for up to 12 processes)
    public static final Color[] GANTT_COLORS = {
        new Color(0x00, 0xE5, 0xFF),  // cyan
        new Color(0x00, 0xFF, 0x88),  // green
        new Color(0xFF, 0xBF, 0x00),  // amber
        new Color(0xBD, 0x93, 0xF9),  // purple
        new Color(0xFF, 0x79, 0xC6),  // pink
        new Color(0x8B, 0xE9, 0xFD),  // sky
        new Color(0xFF, 0xB8, 0x6C),  // orange
        new Color(0x50, 0xFA, 0x7B),  // lime
        new Color(0xFF, 0x55, 0x55),  // red
        new Color(0xF1, 0xFA, 0x8C),  // yellow
        new Color(0x6B, 0xE5, 0xFD),  // ice
        new Color(0xFF, 0xA3, 0x6C),  // peach
    };

    public static final Color GANTT_IDLE = new Color(0x1C, 0x2C, 0x3E);

    // ── Typography ────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE    = new Font("Courier New", Font.BOLD, 22);
    public static final Font FONT_HEADING  = new Font("Courier New", Font.BOLD, 14);
    public static final Font FONT_SUBHEAD  = new Font("Courier New", Font.BOLD, 12);
    public static final Font FONT_BODY     = new Font("Courier New", Font.PLAIN, 12);
    public static final Font FONT_SMALL    = new Font("Courier New", Font.PLAIN, 11);
    public static final Font FONT_CODE     = new Font("Courier New", Font.PLAIN, 12);
    public static final Font FONT_NAV      = new Font("Courier New", Font.BOLD, 13);
    public static final Font FONT_MONO_LG  = new Font("Courier New", Font.BOLD, 15);

    // ── Dimensions ────────────────────────────────────────────────────────────
    public static final int SIDEBAR_W     = 190;
    public static final int HEADER_H      = 56;
    public static final int STATUS_H      = 28;
    public static final int CORNER_RADIUS = 6;
    public static final int PADDING       = 16;
    public static final int GAP           = 8;

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns the display colour for a given process state label. */
    public static Color stateColour(String state) {
        if (state == null) return TEXT_MUTED;
        return switch (state.toUpperCase()) {
            case "NEW"        -> ACCENT_BLUE;
            case "READY"      -> ACCENT_PURPLE;
            case "RUNNING"    -> ACCENT_GREEN;
            case "WAITING"    -> ACCENT_AMBER;
            case "TERMINATED" -> TEXT_MUTED;
            default           -> TEXT_SECONDARY;
        };
    }

    /** Gantt colour for a process by zero-based index. */
    public static Color ganttColour(int index) {
        return GANTT_COLORS[index % GANTT_COLORS.length];
    }

    /** Slightly lighter version of a colour for hover effects. */
    public static Color lighter(Color c, int amount) {
        return new Color(
            Math.min(255, c.getRed()   + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue()  + amount),
            c.getAlpha());
    }

    /** Translucent version. */
    public static Color alpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }
}
