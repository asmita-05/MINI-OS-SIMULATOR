package os.gui;

import os.cli.SimulationContext;
import os.filesystem.FileNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.Optional;

/**
 * Virtual File System panel.
 */
public class FileSystemPanel extends JPanel {

    private final SimulationContext ctx;

    private JTree tree;
    private DefaultTreeModel treeModel;
    private JTextArea contentArea;
    private JLabel lblCwd, lblStatus, lblNodeInfo;

    private JTextField tfPath, tfContent;
    private JButton btnMkdir, btnTouch, btnWrite, btnAppend, btnCat, btnRm;

    public FileSystemPanel(SimulationContext ctx) {
        this.ctx = ctx;
        setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(Theme.GAP, Theme.GAP));
        setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        buildUI();
        refreshTree();
    }

    private void buildUI() {
        add(UIHelper.sectionHeader("VIRTUAL FILE SYSTEM"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildTreePanel(), buildOpsPanel());
        split.setDividerLocation(270);
        split.setDividerSize(4);
        split.setBackground(Theme.BG_PANEL);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        statusBar.setBackground(Theme.BG_PANEL);
        lblCwd = UIHelper.body("/");
        lblCwd.setForeground(Theme.ACCENT_CYAN);
        lblStatus = UIHelper.muted("Ready");
        statusBar.add(UIHelper.muted("cwd:"), BorderLayout.WEST);
        statusBar.add(lblCwd, BorderLayout.CENTER);
        statusBar.add(lblStatus, BorderLayout.EAST);
        statusBar.setBorder(new EmptyBorder(4, 0, 0, 0));
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel buildTreePanel() {
        JPanel p = UIHelper.card();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        p.add(UIHelper.heading("DIRECTORY TREE"), BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("/");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setBackground(Theme.BG_CARD);
        tree.setForeground(Theme.TEXT_PRIMARY);
        tree.setFont(Theme.FONT_CODE);
        tree.setRowHeight(24);
        tree.setOpaque(true);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setBorder(new EmptyBorder(4, 4, 4, 4));
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override public Component getTreeCellRendererComponent(JTree t, Object val,
                    boolean sel, boolean expanded, boolean leaf, int row, boolean focus) {
                JLabel l = (JLabel) super.getTreeCellRendererComponent(t, val, sel, expanded, leaf, row, focus);
                String text = val.toString();
                l.setFont(Theme.FONT_CODE);
                l.setOpaque(true);
                if (sel) {
                    l.setBackground(Theme.BG_SELECTED);
                    l.setForeground(Theme.ACCENT_CYAN);
                } else {
                    l.setBackground(Theme.BG_CARD);
                    l.setForeground(leaf ? Theme.TEXT_CODE : Theme.ACCENT_TEAL);
                }

                boolean isDir = text.endsWith("/") || !leaf;
                l.setIcon(new Icon() {
                    @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        if (isDir) {
                            g2.setColor(sel ? Theme.ACCENT_CYAN : Theme.ACCENT_TEAL);
                            g2.fillRoundRect(x, y + 3, 14, 10, 2, 2);
                            g2.fillRect(x, y + 1, 6, 4);
                        } else {
                            g2.setColor(sel ? Theme.ACCENT_CYAN : Theme.TEXT_CODE);
                            g2.drawRoundRect(x + 1, y + 1, 11, 13, 2, 2);
                            g2.drawLine(x + 3, y + 5, x + 9, y + 5);
                            g2.drawLine(x + 3, y + 8, x + 9, y + 8);
                            g2.drawLine(x + 3, y + 11, x + 7, y + 11);
                        }
                        g2.dispose();
                    }

                    @Override public int getIconWidth() { return 16; }
                    @Override public int getIconHeight() { return 16; }
                });
                setBorder(new EmptyBorder(1, 2, 1, 2));
                return l;
            }
        });

        tree.addTreeSelectionListener(e -> handleTreeSelection());

        JScrollPane scroll = UIHelper.scrollPane(tree);
        scroll.setBorder(null);
        p.add(scroll, BorderLayout.CENTER);

        JPanel treeBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        treeBtns.setBackground(Theme.BG_CARD);
        JButton btnRefresh = UIHelper.ghostButton("Refresh");
        btnRefresh.addActionListener(e -> refreshTree());
        JButton btnCollapse = UIHelper.ghostButton("Collapse");
        btnCollapse.addActionListener(e -> collapseAll());
        treeBtns.add(btnRefresh);
        treeBtns.add(btnCollapse);
        p.add(treeBtns, BorderLayout.SOUTH);

        lblNodeInfo = UIHelper.muted("Click a node to navigate");
        p.add(lblNodeInfo, BorderLayout.NORTH);
        return p;
    }

    private JPanel buildOpsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, Theme.GAP));
        p.setBackground(Theme.BG_PANEL);
        p.add(buildOpsForm(), BorderLayout.NORTH);
        p.add(buildContentView(), BorderLayout.CENTER);
        return p;
    }

    private JPanel buildOpsForm() {
        JPanel card = UIHelper.card();
        card.setLayout(new BorderLayout(0, Theme.GAP));
        card.setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));

        card.add(UIHelper.heading("FILE OPERATIONS"), BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);

        tfPath = UIHelper.textField("/path/to/file or dir");
        tfContent = UIHelper.textField("content (for write/append)");

        fields.add(UIHelper.formRow("Path", tfPath));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("Content", tfContent));

        card.add(fields, BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridLayout(2, 3, 6, 6));
        btns.setOpaque(false);

        btnMkdir = UIHelper.primaryButton("MKDIR");
        btnTouch = UIHelper.primaryButton("TOUCH");
        btnWrite = UIHelper.successButton("WRITE");
        btnAppend = UIHelper.successButton("APPEND");
        btnCat = UIHelper.ghostButton("CAT");
        btnRm = UIHelper.dangerButton("RM");

        btnMkdir.addActionListener(e -> doMkdir());
        btnTouch.addActionListener(e -> doTouch());
        btnWrite.addActionListener(e -> doWrite());
        btnAppend.addActionListener(e -> doAppend());
        btnCat.addActionListener(e -> doCat());
        btnRm.addActionListener(e -> doRm());

        btns.add(btnMkdir);
        btns.add(btnTouch);
        btns.add(btnWrite);
        btns.add(btnAppend);
        btns.add(btnCat);
        btns.add(btnRm);

        card.add(btns, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildContentView() {
        JPanel p = UIHelper.card();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(10, 12, 10, 12));
        p.add(UIHelper.heading("OUTPUT / CONTENT VIEWER"), BorderLayout.NORTH);
        contentArea = UIHelper.consoleArea();
        contentArea.setText("Output will appear here...");
        p.add(UIHelper.scrollPane(contentArea), BorderLayout.CENTER);
        return p;
    }

    private void doMkdir() {
        String path = tfPath.getText().trim();
        if (path.isEmpty()) { flashStatus("Enter a path", Theme.ACCENT_AMBER); return; }
        boolean ok = ctx.getFileSystem().mkdir(path);
        refreshTree();
        flashStatus(ok ? "Directory created: " + path : "mkdir failed: " + path,
                ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
        output(ok ? "Directory created: " + path : "Error: mkdir failed for '" + path + "'");
    }

    private void doTouch() {
        String path = tfPath.getText().trim();
        if (path.isEmpty()) { flashStatus("Enter a path", Theme.ACCENT_AMBER); return; }
        boolean ok = ctx.getFileSystem().touch(path);
        refreshTree();
        flashStatus(ok ? "File created: " + path : "touch failed: " + path,
                ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
        output(ok ? "File created: " + path : "Error: touch failed for '" + path + "'");
    }

    private void doWrite() {
        String path = tfPath.getText().trim();
        String content = tfContent.getText();
        if (path.isEmpty()) { flashStatus("Enter a path", Theme.ACCENT_AMBER); return; }
        boolean ok = ctx.getFileSystem().write(path, content);
        refreshTree();
        flashStatus(ok ? "Written " + content.length() + " chars to " + path : "Write failed",
                ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
        output(ok ? "Written " + content.length() + " chars to: " + path : "Error: write failed");
    }

    private void doAppend() {
        String path = tfPath.getText().trim();
        String content = tfContent.getText();
        boolean ok = ctx.getFileSystem().append(path, content);
        flashStatus(ok ? "Appended to " + path : "Append failed",
                ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
        output(ok ? "Appended " + content.length() + " chars to: " + path : "Error: append failed");
    }

    private void doCat() {
        String path = tfPath.getText().trim();
        Optional<String> c = ctx.getFileSystem().cat(path);
        if (c.isPresent()) {
            output("-- cat " + path + " --\n" + (c.get().isEmpty() ? "(empty file)" : c.get()));
            flashStatus(path, Theme.ACCENT_GREEN);
        } else {
            output("Error: No such file: " + path);
            flashStatus("Not found: " + path, Theme.ACCENT_RED);
        }
    }

    private void doRm() {
        String path = tfPath.getText().trim();
        boolean recursive = path.endsWith("-r") || tfContent.getText().equals("-r");
        if (path.endsWith("-r")) path = path.substring(0, path.length() - 2).trim();
        boolean ok = ctx.getFileSystem().rm(path, recursive);
        refreshTree();
        flashStatus(ok ? "Removed: " + path : "rm failed",
                ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
        output(ok ? "Removed: " + path : "Error: rm failed for '" + path + "'");
    }

    public void refreshTree() {
        DefaultMutableTreeNode root = buildTreeNode(ctx.getFileSystem().getRoot(), "/");
        treeModel.setRoot(root);
        expandAll();
        lblCwd.setText(ctx.getFileSystem().pwd());
    }

    private DefaultMutableTreeNode buildTreeNode(FileNode fn, String displayName) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                fn.isDirectory() ? (displayName.endsWith("/") ? displayName : displayName + "/")
                        : fn.getName());
        if (fn.isDirectory()) {
            for (FileNode child : fn.getChildren()) {
                node.add(buildTreeNode(child, child.getName()));
            }
        }
        return node;
    }

    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
    }

    private void collapseAll() {
        for (int i = tree.getRowCount() - 1; i > 0; i--) tree.collapseRow(i);
    }

    private void handleTreeSelection() {
        TreePath tp = tree.getSelectionPath();
        if (tp == null) return;

        StringBuilder pathBuilder = new StringBuilder();
        Object[] parts = tp.getPath();
        for (int i = 1; i < parts.length; i++) {
            String seg = parts[i].toString().replace("/", "");
            pathBuilder.append("/").append(seg);
        }
        String path = pathBuilder.toString();
        final String resolvedPath = path.isEmpty() ? "/" : path;
        tfPath.setText(resolvedPath);
        lblNodeInfo.setText(resolvedPath);

        Optional<String> c = ctx.getFileSystem().cat(resolvedPath);
        c.ifPresent(content -> output("-- " + resolvedPath + " --\n" + (content.isEmpty() ? "(empty)" : content)));
    }

    private void output(String text) {
        contentArea.setText(text);
        contentArea.setCaretPosition(0);
    }

    private void flashStatus(String msg, Color col) {
        lblStatus.setText(msg);
        lblStatus.setForeground(col);
        javax.swing.Timer t = new javax.swing.Timer(3000, e -> {
            lblStatus.setForeground(Theme.TEXT_MUTED);
            lblStatus.setText("Ready");
        });
        t.setRepeats(false);
        t.start();
    }
}
