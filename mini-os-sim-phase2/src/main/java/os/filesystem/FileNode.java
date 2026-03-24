package os.filesystem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A node in the virtual file system tree.
 *
 * Each node is either a regular FILE or a DIRECTORY.  Directories hold
 * child nodes; files hold text content and a simulated byte size.
 * Both types carry creation / modification timestamps and a simulated
 * inode number.
 */
public class FileNode {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static int inodeCounter = 1;

    // ── Attributes ───────────────────────────────────────────────────────────
    private final int          inode;
    private       String       name;
    private final FileNodeType type;
    private       String       content;      // only meaningful for FILES
    private       int          size;         // bytes (dirs report 0)
    private final LocalDateTime created;
    private       LocalDateTime modified;
    private       FileNode     parent;
    private final List<FileNode> children;   // only populated for DIRECTORY

    // ── Constructors ─────────────────────────────────────────────────────────

    public FileNode(String name, FileNodeType type, FileNode parent) {
        this.inode    = inodeCounter++;
        this.name     = name;
        this.type     = type;
        this.parent   = parent;
        this.content  = "";
        this.size     = 0;
        this.created  = LocalDateTime.now();
        this.modified = this.created;
        this.children = new ArrayList<>();
    }

    // ── Directory operations ─────────────────────────────────────────────────

    public boolean addChild(FileNode child) {
        if (type != FileNodeType.DIRECTORY) return false;
        if (findChild(child.name).isPresent())  return false;
        child.parent = this;
        children.add(child);
        touch();
        return true;
    }

    public boolean removeChild(String childName) {
        Optional<FileNode> opt = findChild(childName);
        if (opt.isEmpty()) return false;
        children.remove(opt.get());
        touch();
        return true;
    }

    public Optional<FileNode> findChild(String childName) {
        return children.stream().filter(c -> c.name.equals(childName)).findFirst();
    }

    public List<FileNode> getChildren() { return Collections.unmodifiableList(children); }

    // ── File operations ──────────────────────────────────────────────────────

    public void writeContent(String text) {
        if (type != FileNodeType.FILE) throw new IllegalStateException("Not a file: " + name);
        this.content  = text;
        this.size     = text.getBytes().length;
        touch();
    }

    public void appendContent(String text) {
        writeContent(content + text);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void touch() { this.modified = LocalDateTime.now(); }

    public boolean isDirectory() { return type == FileNodeType.DIRECTORY; }
    public boolean isFile()      { return type == FileNodeType.FILE; }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int           getInode()    { return inode; }
    public String        getName()     { return name; }
    public FileNodeType  getType()     { return type; }
    public String        getContent()  { return content; }
    public int           getSize()     { return size; }
    public FileNode      getParent()   { return parent; }
    public String        getCreatedFormatted()  { return created.format(FMT); }
    public String        getModifiedFormatted() { return modified.format(FMT); }
    public void          setName(String n)      { this.name = n; touch(); }

    @Override
    public String toString() { return name; }
}
