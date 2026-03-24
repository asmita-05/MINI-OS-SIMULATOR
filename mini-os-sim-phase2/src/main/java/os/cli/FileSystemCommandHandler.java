package os.cli;

import os.filesystem.FileNode;
import os.filesystem.VirtualFileSystem;
import os.utils.CommandInput;
import os.utils.Formatter;

import java.util.List;
import java.util.Optional;

/**
 * Handles virtual file-system commands that mirror a Unix shell subset:
 *
 *   pwd                              — print working directory
 *   cd   <path>                      — change directory
 *   ls   [path]                      — list directory contents
 *   mkdir <path>                     — create directory
 *   touch <path>                     — create empty file
 *   write <path> <content...>        — overwrite file with content
 *   append <path> <content...>       — append to file
 *   cat   <path>                     — print file contents
 *   rm    <path> [-r]                — remove file or directory
 *   mv    <src>  <dst>               — move/rename
 *   find  <name>                     — search for file/dir by name
 *   tree  [path]                     — ASCII directory tree
 */
public class FileSystemCommandHandler implements CommandHandler {

    private static final String[] FS_COMMANDS = {
            "pwd", "cd", "ls", "mkdir", "touch",
            "write", "append", "cat", "rm", "mv", "find", "tree"
    };

    @Override
    public boolean handles(String command) {
        for (String c : FS_COMMANDS) if (c.equals(command)) return true;
        return false;
    }

    @Override
    public String execute(CommandInput input, SimulationContext ctx) {
        VirtualFileSystem fs = ctx.getFileSystem();
        return switch (input.getCommand()) {
            case "pwd"    -> fs.pwd() + "\n";
            case "cd"     -> handleCd(input, fs);
            case "ls"     -> handleLs(input, fs);
            case "mkdir"  -> handleMkdir(input, fs);
            case "touch"  -> handleTouch(input, fs);
            case "write"  -> handleWrite(input, fs);
            case "append" -> handleAppend(input, fs);
            case "cat"    -> handleCat(input, fs);
            case "rm"     -> handleRm(input, fs);
            case "mv"     -> handleMv(input, fs);
            case "find"   -> handleFind(input, fs);
            case "tree"   -> handleTree(input, fs);
            default       -> Formatter.red("Unknown filesystem command.\n");
        };
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    private String handleCd(CommandInput input, VirtualFileSystem fs) {
        String path = input.getArg(0);
        if (path.isEmpty()) { fs.cd("/"); return ""; }
        return fs.cd(path) ? "" : Formatter.red("cd: no such directory: " + path + "\n");
    }

    private String handleLs(CommandInput input, VirtualFileSystem fs) {
        String path = input.getArg(0);
        Optional<List<FileNode>> opt = fs.ls(path);
        if (opt.isEmpty()) return Formatter.red("ls: cannot access '" + path + "'\n");

        List<FileNode> children = opt.get();
        if (children.isEmpty()) return Formatter.yellow("  (empty directory)\n");

        StringBuilder sb = new StringBuilder();
        for (FileNode n : children) {
            String icon    = n.isDirectory() ? Formatter.blue("d") : Formatter.green("-");
            String name    = n.isDirectory() ? Formatter.blue(n.getName() + "/")
                                             : n.getName();
            String sizeStr = n.isFile()
                    ? String.format("%5d B", n.getSize())
                    : "      -";
            sb.append(String.format("%s  %s  %-6s  %s\n",
                    icon, n.getModifiedFormatted(), sizeStr, name));
        }
        return sb.toString();
    }

    private String handleMkdir(CommandInput input, VirtualFileSystem fs) {
        String path = input.getArg(0);
        if (path.isEmpty()) return Formatter.red("Usage: mkdir <path>\n");
        return fs.mkdir(path)
                ? Formatter.green("✓ Directory created: " + path + "\n")
                : Formatter.red("mkdir: failed (path exists or parent not found): " + path + "\n");
    }

    private String handleTouch(CommandInput input, VirtualFileSystem fs) {
        String path = input.getArg(0);
        if (path.isEmpty()) return Formatter.red("Usage: touch <path>\n");
        return fs.touch(path)
                ? Formatter.green("✓ File created: " + path + "\n")
                : Formatter.red("touch: failed: " + path + "\n");
    }

    private String handleWrite(CommandInput input, VirtualFileSystem fs) {
        if (input.getArgCount() < 2) return Formatter.red("Usage: write <path> <content...>\n");
        String path    = input.getArg(0);
        String content = input.joinArgs(1);
        return fs.write(path, content)
                ? Formatter.green("✓ Written " + content.length() + " chars to " + path + "\n")
                : Formatter.red("write: failed: " + path + "\n");
    }

    private String handleAppend(CommandInput input, VirtualFileSystem fs) {
        if (input.getArgCount() < 2) return Formatter.red("Usage: append <path> <content...>\n");
        String path    = input.getArg(0);
        String content = input.joinArgs(1);
        return fs.append(path, content)
                ? Formatter.green("✓ Appended " + content.length() + " chars to " + path + "\n")
                : Formatter.red("append: failed (file not found): " + path + "\n");
    }

    private String handleCat(CommandInput input, VirtualFileSystem fs) {
        String path = input.getArg(0);
        if (path.isEmpty()) return Formatter.red("Usage: cat <path>\n");
        Optional<String> content = fs.cat(path);
        if (content.isEmpty()) return Formatter.red("cat: " + path + ": No such file\n");
        return content.get().isEmpty()
                ? Formatter.yellow("(empty file)\n")
                : content.get() + "\n";
    }

    private String handleRm(CommandInput input, VirtualFileSystem fs) {
        if (input.getArgCount() < 1) return Formatter.red("Usage: rm <path> [-r]\n");
        boolean recursive = false;
        String  path      = "";
        // Parse flags
        for (int i = 0; i < input.getArgCount(); i++) {
            String a = input.getArg(i);
            if (a.equals("-r") || a.equals("-rf")) recursive = true;
            else path = a;
        }
        if (path.isEmpty()) return Formatter.red("Usage: rm <path> [-r]\n");
        return fs.rm(path, recursive)
                ? Formatter.green("✓ Removed: " + path + "\n")
                : Formatter.red("rm: failed (not found, non-empty dir, or cannot remove root): " + path + "\n");
    }

    private String handleMv(CommandInput input, VirtualFileSystem fs) {
        if (input.getArgCount() < 2) return Formatter.red("Usage: mv <src> <dst>\n");
        String src = input.getArg(0);
        String dst = input.getArg(1);
        return fs.mv(src, dst)
                ? Formatter.green("✓ Moved '" + src + "' → '" + dst + "'\n")
                : Formatter.red("mv: failed: " + src + " → " + dst + "\n");
    }

    private String handleFind(CommandInput input, VirtualFileSystem fs) {
        String name = input.getArg(0);
        if (name.isEmpty()) return Formatter.red("Usage: find <name>\n");
        List<String> results = fs.find(name);
        if (results.isEmpty()) return Formatter.yellow("No matches for '" + name + "'.\n");
        StringBuilder sb = new StringBuilder();
        results.forEach(r -> sb.append("  ").append(r).append("\n"));
        return sb.toString();
    }

    private String handleTree(CommandInput input, VirtualFileSystem fs) {
        String path = input.getArg(0);
        Optional<FileNode> opt = fs.resolve(path.isEmpty() ? "." : path);
        if (opt.isEmpty() || !opt.get().isDirectory())
            return Formatter.red("tree: not a directory: " + path + "\n");

        StringBuilder sb = new StringBuilder();
        sb.append(Formatter.cyan(opt.get().getName())).append("\n");
        printTree(opt.get(), "", sb);
        return sb.toString();
    }

    private void printTree(FileNode node, String prefix, StringBuilder sb) {
        List<FileNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            FileNode child = children.get(i);
            boolean  last  = i == children.size() - 1;
            sb.append(prefix).append(last ? "└── " : "├── ");
            if (child.isDirectory()) sb.append(Formatter.blue(child.getName() + "/"));
            else                     sb.append(Formatter.green(child.getName()))
                                       .append(Formatter.yellow(" (" + child.getSize() + " B)"));
            sb.append("\n");
            if (child.isDirectory())
                printTree(child, prefix + (last ? "    " : "│   "), sb);
        }
    }

    @Override
    public String helpText() {
        return "pwd | cd <p> | ls [p] | mkdir <p> | touch <p> | write <p> <content> | " +
               "append <p> <c> | cat <p> | rm <p> [-r] | mv <src> <dst> | find <n> | tree [p]";
    }
}
