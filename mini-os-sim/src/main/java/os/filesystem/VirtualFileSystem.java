package os.filesystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Virtual File System (VFS) manager.
 *
 * Maintains a rooted tree of {@link FileNode} objects that models a Unix-style
 * hierarchical file system.  Operations are path-based; absolute paths start
 * with '/', relative paths are resolved against the current working directory.
 *
 * Supported operations:
 *   mkdir, touch, write, append, cat, ls, cd, pwd, rm, mv, find
 */
public class VirtualFileSystem {

    private static final String SEPARATOR = "/";

    private final FileNode root;
    private       FileNode cwd;        // current working directory

    public VirtualFileSystem() {
        this.root = new FileNode("/", FileNodeType.DIRECTORY, null);
        this.cwd  = root;
        // Seed a minimal directory tree
        mkdir("/bin");
        mkdir("/home");
        mkdir("/tmp");
        mkdir("/etc");
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    public String pwd() {
        if (cwd == root) return "/";
        List<String> parts = new ArrayList<>();
        FileNode cur = cwd;
        while (cur != root) { parts.add(0, cur.getName()); cur = cur.getParent(); }
        return "/" + String.join("/", parts);
    }

    public boolean cd(String path) {
        Optional<FileNode> opt = resolve(path);
        if (opt.isEmpty() || !opt.get().isDirectory()) return false;
        cwd = opt.get();
        return true;
    }

    // ── Directory operations ─────────────────────────────────────────────────

    public boolean mkdir(String path) {
        String[] parts  = splitPath(path);
        String   newDir = parts[1];
        if (newDir == null || newDir.isBlank()) return false;

        Optional<FileNode> parentOpt = resolve(parts[0]);
        if (parentOpt.isEmpty() || !parentOpt.get().isDirectory()) return false;

        FileNode parent = parentOpt.get();
        if (parent.findChild(newDir).isPresent()) return false;   // already exists

        return parent.addChild(new FileNode(newDir, FileNodeType.DIRECTORY, parent));
    }

    /** Creates all missing directories in path (like mkdir -p). */
    public boolean mkdirp(String path) {
        String[] tokens = normalisePath(path).split("/");
        FileNode cur    = root;
        for (String token : tokens) {
            if (token.isBlank()) continue;
            Optional<FileNode> child = cur.findChild(token);
            if (child.isPresent()) {
                if (!child.get().isDirectory()) return false;
                cur = child.get();
            } else {
                FileNode dir = new FileNode(token, FileNodeType.DIRECTORY, cur);
                cur.addChild(dir);
                cur = dir;
            }
        }
        return true;
    }

    // ── File operations ──────────────────────────────────────────────────────

    public boolean touch(String path) {
        String[] parts    = splitPath(path);
        String   filename = parts[1];
        if (filename == null || filename.isBlank()) return false;

        Optional<FileNode> parentOpt = resolve(parts[0]);
        if (parentOpt.isEmpty() || !parentOpt.get().isDirectory()) return false;

        FileNode parent = parentOpt.get();
        if (parent.findChild(filename).isPresent()) return true; // already exists

        return parent.addChild(new FileNode(filename, FileNodeType.FILE, parent));
    }

    public boolean write(String path, String content) {
        touch(path);
        Optional<FileNode> opt = resolve(path);
        if (opt.isEmpty() || !opt.get().isFile()) return false;
        opt.get().writeContent(content);
        return true;
    }

    public boolean append(String path, String content) {
        Optional<FileNode> opt = resolve(path);
        if (opt.isEmpty() || !opt.get().isFile()) return false;
        opt.get().appendContent(content);
        return true;
    }

    public Optional<String> cat(String path) {
        Optional<FileNode> opt = resolve(path);
        if (opt.isEmpty() || !opt.get().isFile()) return Optional.empty();
        return Optional.of(opt.get().getContent());
    }

    public boolean rm(String path, boolean recursive) {
        Optional<FileNode> opt = resolve(path);
        if (opt.isEmpty()) return false;

        FileNode node   = opt.get();
        FileNode parent = node.getParent();
        if (parent == null) return false; // can't remove root

        if (node.isDirectory() && !node.getChildren().isEmpty() && !recursive) return false;
        return parent.removeChild(node.getName());
    }

    public boolean mv(String srcPath, String destPath) {
        Optional<FileNode> srcOpt = resolve(srcPath);
        if (srcOpt.isEmpty()) return false;

        FileNode src       = srcOpt.get();
        FileNode srcParent = src.getParent();
        if (srcParent == null) return false;

        // Destination could be either a new name or an existing directory
        Optional<FileNode> destOpt = resolve(destPath);
        if (destOpt.isPresent() && destOpt.get().isDirectory()) {
            srcParent.removeChild(src.getName());
            destOpt.get().addChild(src);
        } else {
            // Rename: dest = new name under same parent or a path
            String[] parts = splitPath(destPath);
            Optional<FileNode> newParentOpt = resolve(parts[0]);
            if (newParentOpt.isEmpty() || !newParentOpt.get().isDirectory()) return false;
            srcParent.removeChild(src.getName());
            src.setName(parts[1]);
            newParentOpt.get().addChild(src);
        }
        return true;
    }

    // ── Listing ──────────────────────────────────────────────────────────────

    public Optional<List<FileNode>> ls(String path) {
        Optional<FileNode> opt = resolve(path.isBlank() ? "." : path);
        if (opt.isEmpty() || !opt.get().isDirectory()) return Optional.empty();
        return Optional.of(opt.get().getChildren());
    }

    public Optional<List<FileNode>> ls() { return ls("."); }

    // ── Search ───────────────────────────────────────────────────────────────

    public List<String> find(String name) {
        List<String> results = new ArrayList<>();
        findRecursive(root, name, "/", results);
        return results;
    }

    private void findRecursive(FileNode node, String name, String currentPath, List<String> results) {
        for (FileNode child : node.getChildren()) {
            String childPath = currentPath.equals("/") ? "/" + child.getName()
                                                       : currentPath + "/" + child.getName();
            if (child.getName().contains(name)) results.add(childPath);
            if (child.isDirectory()) findRecursive(child, name, childPath, results);
        }
    }

    // ── Path resolution ──────────────────────────────────────────────────────

    /**
     * Resolves an absolute or relative path to a FileNode.
     *
     * Special tokens: "." = current, ".." = parent, "/" = root
     */
    public Optional<FileNode> resolve(String path) {
        if (path == null || path.isBlank() || path.equals(".")) return Optional.of(cwd);
        if (path.equals("/")) return Optional.of(root);
        if (path.equals("..")) return Optional.ofNullable(cwd.getParent() == null ? cwd : cwd.getParent());

        String   normalised = normalisePath(path);
        String[] tokens     = normalised.split("/");
        FileNode cur        = normalised.startsWith("/") ? root : cwd;

        for (String token : tokens) {
            if (token.isBlank() || token.equals(".")) continue;
            if (token.equals("..")) {
                cur = cur.getParent() == null ? cur : cur.getParent();
                continue;
            }
            Optional<FileNode> child = cur.findChild(token);
            if (child.isEmpty()) return Optional.empty();
            cur = child.get();
        }
        return Optional.of(cur);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /** Splits a path into [parent-path, last-component]. */
    private String[] splitPath(String path) {
        String norm = normalisePath(path);
        int    idx  = norm.lastIndexOf('/');
        if (idx < 0) return new String[]{".", norm};
        String parent = norm.substring(0, idx);
        String name   = norm.substring(idx + 1);
        return new String[]{ parent.isEmpty() ? "/" : parent, name };
    }

    private String normalisePath(String path) {
        return path.replace("\\", "/");
    }

    public FileNode getRoot() { return root; }
    public FileNode getCwd()  { return cwd; }
}
