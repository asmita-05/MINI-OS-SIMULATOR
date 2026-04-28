package os.gui;

import os.cli.SimulationContext;
import os.ipc.IPCMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Inter-process communication panel for direct messages and named channels.
 */
public class IPCPanel extends JPanel {

    private final SimulationContext ctx;

    private JSpinner spFromPid, spToPid, spInboxPid, spChannelPid;
    private JTextField tfMessage, tfChannel, tfChannelMessage;
    private JTextArea infoArea;
    private DefaultTableModel inboxModel;
    private DefaultTableModel channelModel;
    private JLabel lblStatus;

    public IPCPanel(SimulationContext ctx) {
        this.ctx = ctx;
        setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(Theme.GAP, Theme.GAP));
        setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        buildUI();
        refresh();
        new javax.swing.Timer(1000, e -> refresh()).start();
    }

    private void buildUI() {
        add(UIHelper.sectionHeader("INTER-PROCESS COMMUNICATION"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildControls(), buildMonitor());
        split.setDividerLocation(360);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(Theme.BG_PANEL);
        add(split, BorderLayout.CENTER);

        lblStatus = UIHelper.muted("Use Java-thread-backed processes to exchange mailbox and channel messages.");
        lblStatus.setBorder(new EmptyBorder(4, 0, 0, 0));
        add(lblStatus, BorderLayout.SOUTH);
    }

    private JPanel buildControls() {
        JPanel wrap = new JPanel(new BorderLayout(0, Theme.GAP));
        wrap.setBackground(Theme.BG_PANEL);
        wrap.add(buildDirectCard(), BorderLayout.NORTH);
        wrap.add(buildChannelCard(), BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildDirectCard() {
        JPanel card = UIHelper.card();
        card.setLayout(new BorderLayout(0, Theme.GAP));
        card.setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        card.add(UIHelper.heading("DIRECT MAILBOX"), BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);

        spFromPid = UIHelper.intSpinner(1, 1, 9999);
        spToPid   = UIHelper.intSpinner(1, 1, 9999);
        spInboxPid = UIHelper.intSpinner(1, 1, 9999);
        tfMessage = UIHelper.textField("message payload");

        fields.add(UIHelper.formRow("From PID", spFromPid));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("To PID", spToPid));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("Message", tfMessage));
        fields.add(Box.createVerticalStrut(10));

        JButton btnSend = UIHelper.successButton("SEND");
        btnSend.addActionListener(e -> sendDirect());
        JButton btnRecv = UIHelper.primaryButton("RECV SELECTED");
        btnRecv.addActionListener(e -> receiveInbox());

        JPanel btns = new JPanel(new GridLayout(1, 2, 6, 0));
        btns.setOpaque(false);
        btns.add(btnSend);
        btns.add(btnRecv);

        fields.add(btns);
        fields.add(Box.createVerticalStrut(12));
        fields.add(UIHelper.formRow("Inbox PID", spInboxPid));

        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildChannelCard() {
        JPanel card = UIHelper.card();
        card.setLayout(new BorderLayout(0, Theme.GAP));
        card.setBorder(new EmptyBorder(Theme.PADDING, Theme.PADDING, Theme.PADDING, Theme.PADDING));
        card.add(UIHelper.heading("NAMED CHANNELS"), BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);

        tfChannel = UIHelper.textField("channel name");
        tfChannelMessage = UIHelper.textField("channel payload");
        spChannelPid = UIHelper.intSpinner(1, 1, 9999);

        fields.add(UIHelper.formRow("Channel", tfChannel));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("Payload", tfChannelMessage));
        fields.add(Box.createVerticalStrut(6));
        fields.add(UIHelper.formRow("Read As PID", spChannelPid));
        fields.add(Box.createVerticalStrut(10));

        JButton btnCreate = UIHelper.primaryButton("CREATE CHANNEL");
        btnCreate.addActionListener(e -> createChannel());
        JButton btnPublish = UIHelper.successButton("PUBLISH");
        btnPublish.addActionListener(e -> publishChannel());
        JButton btnPeek = UIHelper.ghostButton("REFRESH CHANNEL");
        btnPeek.addActionListener(e -> refresh());
        JButton btnConsume = UIHelper.primaryButton("CONSUME");
        btnConsume.addActionListener(e -> consumeChannel());

        JPanel btns = new JPanel(new GridLayout(2, 2, 6, 6));
        btns.setOpaque(false);
        btns.add(btnCreate);
        btns.add(btnPublish);
        btns.add(btnPeek);
        btns.add(btnConsume);
        fields.add(btns);

        card.add(fields, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildMonitor() {
        JPanel wrap = new JPanel(new BorderLayout(0, Theme.GAP));
        wrap.setBackground(Theme.BG_PANEL);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildInboxTable(), buildChannelTable());
        split.setDividerLocation(220);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(Theme.BG_PANEL);
        wrap.add(split, BorderLayout.CENTER);
        wrap.add(buildInfoCard(), BorderLayout.SOUTH);
        return wrap;
    }

    private JPanel buildInboxTable() {
        JPanel card = UIHelper.card();
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.add(UIHelper.heading("MAILBOX SNAPSHOT"), BorderLayout.NORTH);

        String[] cols = {"ID", "From", "To/Channel", "Time", "Payload"};
        inboxModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(inboxModel);
        UIHelper.styleTable(table);
        card.add(UIHelper.scrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildChannelTable() {
        JPanel card = UIHelper.card();
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.add(UIHelper.heading("CHANNEL SNAPSHOT"), BorderLayout.NORTH);

        String[] cols = {"ID", "From", "Channel", "Time", "Payload"};
        channelModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(channelModel);
        UIHelper.styleTable(table);
        card.add(UIHelper.scrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInfoCard() {
        JPanel card = UIHelper.card();
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.setPreferredSize(new Dimension(100, 170));
        card.add(UIHelper.heading("IPC OVERVIEW"), BorderLayout.NORTH);
        infoArea = UIHelper.consoleArea();
        card.add(UIHelper.scrollPane(infoArea), BorderLayout.CENTER);
        return card;
    }

    private void sendDirect() {
        try {
            int fromPid = (Integer) spFromPid.getValue();
            int toPid   = (Integer) spToPid.getValue();
            String payload = tfMessage.getText().trim();
            if (payload.isEmpty()) {
                flashStatus("Enter a message payload", Theme.ACCENT_AMBER);
                return;
            }
            ctx.getIpcManager().sendDirect(fromPid, toPid, payload);
            tfMessage.setText("");
            refresh();
            flashStatus("Message sent from P" + fromPid + " to P" + toPid, Theme.ACCENT_GREEN);
        } catch (Exception ex) {
            flashStatus(ex.getMessage(), Theme.ACCENT_RED);
        }
    }

    private void receiveInbox() {
        try {
            int pid = (Integer) spInboxPid.getValue();
            var opt = ctx.getIpcManager().receiveDirect(pid);
            refresh();
            flashStatus(opt.isPresent() ? "P" + pid + " received message #" + opt.get().id() : "Inbox is empty",
                    opt.isPresent() ? Theme.ACCENT_GREEN : Theme.ACCENT_AMBER);
        } catch (Exception ex) {
            flashStatus(ex.getMessage(), Theme.ACCENT_RED);
        }
    }

    private void createChannel() {
        try {
            boolean created = ctx.getIpcManager().createChannel(tfChannel.getText());
            refresh();
            flashStatus(created ? "Channel created" : "Channel already exists",
                    created ? Theme.ACCENT_CYAN : Theme.ACCENT_AMBER);
        } catch (Exception ex) {
            flashStatus(ex.getMessage(), Theme.ACCENT_RED);
        }
    }

    private void publishChannel() {
        try {
            String channel = tfChannel.getText().trim();
            String payload = tfChannelMessage.getText().trim();
            int fromPid = (Integer) spFromPid.getValue();
            if (channel.isEmpty() || payload.isEmpty()) {
                flashStatus("Enter a channel and payload", Theme.ACCENT_AMBER);
                return;
            }
            ctx.getIpcManager().publish(channel, fromPid, payload);
            tfChannelMessage.setText("");
            refresh();
            flashStatus("Published on channel '" + channel.toLowerCase() + "'", Theme.ACCENT_GREEN);
        } catch (Exception ex) {
            flashStatus(ex.getMessage(), Theme.ACCENT_RED);
        }
    }

    private void consumeChannel() {
        try {
            String channel = tfChannel.getText().trim();
            int pid = (Integer) spChannelPid.getValue();
            if (channel.isEmpty()) {
                flashStatus("Enter a channel name first", Theme.ACCENT_AMBER);
                return;
            }
            var opt = ctx.getIpcManager().consumeChannel(channel, pid);
            refresh();
            flashStatus(opt.isPresent()
                            ? "P" + pid + " consumed channel message #" + opt.get().id()
                            : "Channel '" + channel.toLowerCase() + "' is empty",
                    opt.isPresent() ? Theme.ACCENT_GREEN : Theme.ACCENT_AMBER);
        } catch (Exception ex) {
            flashStatus(ex.getMessage(), Theme.ACCENT_RED);
        }
    }

    private void refresh() {
        int pid = (Integer) spInboxPid.getValue();
        List<IPCMessage> messages;
        try {
            messages = ctx.getIpcManager().peekMailbox(pid, 25);
        } catch (Exception ignored) {
            messages = List.of();
        }

        inboxModel.setRowCount(0);
        for (IPCMessage msg : messages) {
            inboxModel.addRow(new Object[]{
                    msg.id(),
                    "P" + msg.fromPid(),
                    msg.toPid() == null ? msg.channel() : "P" + msg.toPid(),
                    msg.timestampLabel(),
                    msg.payload()
            });
        }

        List<IPCMessage> channelMessages;
        String activeChannel = tfChannel.getText().trim();
        try {
            channelMessages = activeChannel.isEmpty()
                    ? List.of()
                    : ctx.getIpcManager().peekChannel(activeChannel, 25);
        } catch (Exception ignored) {
            channelMessages = List.of();
        }

        channelModel.setRowCount(0);
        for (IPCMessage msg : channelMessages) {
            channelModel.addRow(new Object[] {
                    msg.id(),
                    "P" + msg.fromPid(),
                    msg.channel(),
                    msg.timestampLabel(),
                    msg.payload()
            });
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Pending messages : ").append(ctx.getIpcManager().getTotalPendingMessages()).append("\n");
        sb.append("Known channels   : ").append(ctx.getIpcManager().listChannels().size()).append("\n");
        sb.append("Channels         : ").append(ctx.getIpcManager().listChannels()).append("\n\n");
        if (!activeChannel.isEmpty()) {
            try {
                sb.append("Active channel '").append(activeChannel.toLowerCase()).append("' pending : ")
                        .append(ctx.getIpcManager().getPendingChannelCount(activeChannel)).append("\n\n");
            } catch (Exception ignored) {}
        }
        sb.append("Process IPC stats\n");
        for (os.process.Process p : ctx.getProcessManager().getAll()) {
            sb.append(String.format("P%-3d sent=%-4d recv=%-4d thread=%s%n",
                    p.getPid(), p.getMessagesSent(), p.getMessagesReceived(), p.getJavaThreadState()));
        }
        infoArea.setText(sb.toString());
        infoArea.setCaretPosition(0);
    }

    private void flashStatus(String msg, Color color) {
        lblStatus.setText(msg);
        lblStatus.setForeground(color);
        javax.swing.Timer timer = new javax.swing.Timer(3000, e -> {
            lblStatus.setText("Use Java-thread-backed processes to exchange mailbox and channel messages.");
            lblStatus.setForeground(Theme.TEXT_MUTED);
        });
        timer.setRepeats(false);
        timer.start();
    }
}
