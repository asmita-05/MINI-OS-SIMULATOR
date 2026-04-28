package os.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bounded circular command history.
 *
 * Stores the last {@code maxSize} non-blank commands entered by the user.
 * The cursor tracks position for up/down navigation (shell arrow-key emulation).
 * Duplicate consecutive entries are suppressed.
 */
public class CommandHistory {

    private final int          maxSize;
    private final List<String> history;
    private       int          cursor;    // -1 = at the live prompt

    public CommandHistory(int maxSize) {
        this.maxSize = maxSize;
        this.history = new ArrayList<>(maxSize);
        this.cursor  = -1;
    }

    /** Adds a command to history. Suppresses blank input and consecutive duplicates. */
    public void add(String command) {
        if (command == null || command.isBlank()) return;
        if (!history.isEmpty() && history.get(history.size() - 1).equals(command)) return;

        if (history.size() >= maxSize) history.remove(0);
        history.add(command);
        cursor = -1; // reset cursor to live prompt
    }

    /** Returns the previous command (moving backward in history). */
    public String previous() {
        if (history.isEmpty()) return "";
        if (cursor == -1)       cursor = history.size() - 1;
        else if (cursor > 0)    cursor--;
        return history.get(cursor);
    }

    /** Returns the next command (moving forward toward the live prompt). */
    public String next() {
        if (cursor == -1 || cursor >= history.size() - 1) { cursor = -1; return ""; }
        cursor++;
        return history.get(cursor);
    }

    /** Returns all history entries (oldest → newest). */
    public List<String> getAll() { return Collections.unmodifiableList(history); }

    public int size()  { return history.size(); }
    public void clear(){ history.clear(); cursor = -1; }

    /** Formats history as a numbered list for display. */
    public String format() {
        if (history.isEmpty()) return "  (no history)\n";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < history.size(); i++)
            sb.append(String.format("  %3d  %s\n", i + 1, history.get(i)));
        return sb.toString();
    }
}
