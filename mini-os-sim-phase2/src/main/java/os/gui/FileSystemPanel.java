package os.gui;

import os.cli.SimulationContext;
import os.filesystem.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Optional;

/**
 * Virtual File System panel — Phase 2.
 *
 * Implemented : mkdir, touch, write, cat, ls, pwd, cd, rm (files only).
 * Phase 3     : mv, find, tree, append, rm -r (recursive), advanced path resolution.
 */
public class FileSystemPanel extends JPanel {

    private final SimulationContext ctx;

    private JTree             tree;
    private DefaultTreeModel  treeModel;
    private JTextArea         contentArea;
    private JLabel            lblCwd, lblStatus, lblNodeInfo;
    private JTextField        tfPath, tfContent;

    public FileSystemPanel(SimulationContext ctx) {
        this.ctx = ctx;
        setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(Theme.GAP, Theme.GAP));
        setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        buildUI();
        refreshTree();
    }

    private void buildUI() {
        add(UIHelper.sectionHeader("⬡  VIRTUAL FILE SYSTEM"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildTreePanel(), buildOpsPanel());
        split.setDividerLocation(260);
        split.setDividerSize(4);
        split.setBackground(Theme.BG_PANEL);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new BorderLayout(8, 0));
        statusBar.setBackground(Theme.BG_PANEL);
        lblCwd    = UIHelper.body("/");
        lblCwd.setForeground(Theme.ACCENT_CYAN);
        lblStatus = UIHelper.muted("Ready");
        statusBar.add(UIHelper.muted("cwd:"), BorderLayout.WEST);
        statusBar.add(lblCwd, BorderLayout.CENTER);
        statusBar.add(lblStatus, BorderLayout.EAST);
        statusBar.setBorder(new EmptyBorder(4,0,0,0));
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel buildTreePanel() {
        JPanel p = UIHelper.card();
        p.setLayout(new BorderLayout(0, 6));
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        lblNodeInfo = UIHelper.muted("Click a node to navigate");
        p.add(UIHelper.heading("DIRECTORY TREE"), BorderLayout.NORTH);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("/");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setBackground(Theme.BG_CARD);
        tree.setForeground(Theme.TEXT_PRIMARY);
        tree.setFont(Theme.FONT_CODE);
        tree.setRowHeight(24);
        tree.setOpaque(true);
        tree.setBorder(new EmptyBorder(4,4,4,4));
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree t, Object val,
                    boolean sel, boolean exp, boolean leaf, int row, boolean foc) {
                JLabel l = (JLabel) super.getTreeCellRendererComponent(t,val,sel,exp,leaf,row,foc);
                l.setFont(Theme.FONT_CODE);
                l.setOpaque(true);
                l.setBackground(sel ? Theme.BG_SELECTED : Theme.BG_CARD);
                l.setForeground(sel ? Theme.ACCENT_CYAN : (leaf ? Theme.TEXT_CODE : Theme.ACCENT_TEAL));
                l.setIcon(null);
                l.setText((leaf ? "  📄 " : "  📁 ") + val.toString().replace("/",""));
                return l;
            }
        });
        tree.addTreeSelectionListener(e -> handleSelection());
        JScrollPane scroll = UIHelper.scrollPane(tree);
        scroll.setBorder(null);
        p.add(scroll, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setBackground(Theme.BG_CARD);
        JButton btnRefresh = UIHelper.ghostButton("↻ Refresh");
        btnRefresh.addActionListener(e -> refreshTree());
        btns.add(btnRefresh);
        p.add(btns, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildOpsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, Theme.GAP));
        p.setBackground(Theme.BG_PANEL);
        p.add(buildOpsForm(),     BorderLayout.NORTH);
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
        tfPath    = UIHelper.textField("/path/to/file or dir");
        tfContent = UIHelper.textField("content (for write)");
        fields.add(UIHelper.formRow("Path", tfPath));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("Content", tfContent));
        card.add(fields, BorderLayout.CENTER);

        // Phase 2: 5 working buttons
        JPanel implemented = new JPanel(new GridLayout(1, 5, 6, 0));
        implemented.setOpaque(false);
        JButton btnMkdir = UIHelper.primaryButton("MKDIR");
        JButton btnTouch = UIHelper.primaryButton("TOUCH");
        JButton btnWrite = UIHelper.successButton("WRITE");
        JButton btnCat   = UIHelper.ghostButton("CAT");
        JButton btnRm    = UIHelper.dangerButton("RM");
        btnMkdir.addActionListener(e -> doMkdir());
        btnTouch.addActionListener(e -> doTouch());
        btnWrite.addActionListener(e -> doWrite());
        btnCat  .addActionListener(e -> doCat());
        btnRm   .addActionListener(e -> doRm());
        implemented.add(btnMkdir); implemented.add(btnTouch);
        implemented.add(btnWrite); implemented.add(btnCat);
        implemented.add(btnRm);

        // Phase 3 placeholder row
        JPanel phase3row = new JPanel(new GridLayout(1, 4, 6, 0));
        phase3row.setOpaque(false);
        for (String lbl : new String[]{"APPEND [P3]", "MV [P3]", "FIND [P3]", "TREE [P3]"}) {
            JButton btn = UIHelper.ghostButton(lbl);
            btn.setForeground(Theme.ACCENT_AMBER);
            btn.addActionListener(e -> flashStatus("⚠ Coming in Phase 3", Theme.ACCENT_AMBER));
            phase3row.add(btn);
        }

        JPanel btns = new JPanel(new BorderLayout(0, 6));
        btns.setOpaque(false);
        btns.add(implemented, BorderLayout.NORTH);
        btns.add(phase3row,   BorderLayout.CENTER);

        JLabel p3note = UIHelper.muted("Buttons labelled [P3] are planned for Phase 3");
        p3note.setBorder(new EmptyBorder(2,0,0,0));
        btns.add(p3note, BorderLayout.SOUTH);
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

    // ── Operations ────────────────────────────────────────────────────────────

    private void doMkdir() {
        String path = tfPath.getText().trim();
        if (path.isEmpty()) { flashStatus("Enter a path", Theme.ACCENT_AMBER); return; }
        boolean ok = ctx.getFileSystem().mkdir(path);
        refreshTree();
        output(ok ? "✓ Directory created: " + path : "Error: mkdir failed for '" + path + "'");
        flashStatus(ok ? "✓ Directory created" : "⚠ mkdir failed", ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
    }

    private void doTouch() {
        String path = tfPath.getText().trim();
        if (path.isEmpty()) { flashStatus("Enter a path", Theme.ACCENT_AMBER); return; }
        boolean ok = ctx.getFileSystem().touch(path);
        refreshTree();
        output(ok ? "✓ File created: " + path : "Error: touch failed for '" + path + "'");
        flashStatus(ok ? "✓ File created" : "⚠ touch failed", ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
    }

    private void doWrite() {
        String path = tfPath.getText().trim(), content = tfContent.getText();
        if (path.isEmpty()) { flashStatus("Enter a path", Theme.ACCENT_AMBER); return; }
        boolean ok = ctx.getFileSystem().write(path, content);
        refreshTree();
        output(ok ? "✓ Written " + content.length() + " chars to: " + path : "Error: write failed");
        flashStatus(ok ? "✓ Written" : "⚠ Write failed", ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
    }

    private void doCat() {
        String path = tfPath.getText().trim();
        Optional<String> c = ctx.getFileSystem().cat(path);
        if (c.isPresent()) {
            output("── cat " + path + " ──\n" + (c.get().isEmpty() ? "(empty file)" : c.get()));
            flashStatus("✓ " + path, Theme.ACCENT_GREEN);
        } else {
            output("Error: No such file: " + path);
            flashStatus("⚠ Not found", Theme.ACCENT_RED);
        }
    }

    private void doRm() {
        String path = tfPath.getText().trim();
        boolean ok = ctx.getFileSystem().rm(path, false);
        refreshTree();
        output(ok ? "✓ Removed: " + path : "Error: rm failed (use Phase 3 for recursive rm)");
        flashStatus(ok ? "✓ Removed" : "⚠ rm failed", ok ? Theme.ACCENT_GREEN : Theme.ACCENT_RED);
    }

    // ── Tree ──────────────────────────────────────────────────────────────────

    public void refreshTree() {
        DefaultMutableTreeNode root = buildNode(ctx.getFileSystem().getRoot(), "/");
        treeModel.setRoot(root);
        for (int i = 0; i < tree.getRowCount(); i++) tree.expandRow(i);
        lblCwd.setText(ctx.getFileSystem().pwd());
    }

    private DefaultMutableTreeNode buildNode(FileNode fn, String name) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
                fn.isDirectory() ? name + "/" : fn.getName());
        if (fn.isDirectory())
            for (FileNode child : fn.getChildren())
                node.add(buildNode(child, child.getName()));
        return node;
    }

    private void handleSelection() {
        TreePath tp = tree.getSelectionPath();
        if (tp == null) return;
        StringBuilder pb = new StringBuilder();
        for (int i = 1; i < tp.getPath().length; i++)
            pb.append("/").append(tp.getPath()[i].toString().replace("/",""));
        String path = pb.toString().isEmpty() ? "/" : pb.toString();
        tfPath.setText(path);
        Optional<String> c = ctx.getFileSystem().cat(path);
        c.ifPresent(content -> output("── " + path + " ──\n" + (content.isEmpty() ? "(empty)" : content)));
    }

    private void output(String text) { contentArea.setText(text); contentArea.setCaretPosition(0); }

    private void flashStatus(String msg, Color col) {
        lblStatus.setText(msg); lblStatus.setForeground(col);
        javax.swing.Timer t = new javax.swing.Timer(3000, e -> {
            lblStatus.setForeground(Theme.TEXT_MUTED);
            lblStatus.setText("Ready");
        });
        t.setRepeats(false); t.start();
    }
}
