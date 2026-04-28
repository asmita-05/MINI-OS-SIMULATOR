package os.cli;

import os.ipc.IPCManager;
import os.ipc.IPCMessage;
import os.utils.CommandInput;
import os.utils.Formatter;

import java.util.List;
import java.util.Optional;

/**
 * Handles inter-process communication commands.
 */
public class IPCCommandHandler implements CommandHandler {

    @Override
    public boolean handles(String command) {
        return command.equals("ipc");
    }

    @Override
    public String execute(CommandInput input, SimulationContext ctx) {
        String sub = input.getArg(0).toLowerCase();
        return switch (sub) {
            case "send"               -> handleSend(input, ctx.getIpcManager());
            case "recv", "receive"    -> handleReceive(input, ctx.getIpcManager());
            case "inbox", "peek"      -> handleInbox(input, ctx.getIpcManager());
            case "mkchan", "channel"  -> handleCreateChannel(input, ctx.getIpcManager());
            case "publish", "pub"     -> handlePublish(input, ctx.getIpcManager());
            case "readchan", "chan"   -> handleReadChannel(input, ctx.getIpcManager());
            case "channels", "list"   -> handleChannels(ctx.getIpcManager());
            case "info", "status", "" -> handleInfo(ctx.getIpcManager(), ctx);
            default                   -> Formatter.red(
                    "Unknown sub-command '" + sub + "'. Use: send | recv | inbox | mkchan | publish | readchan | channels | info\n");
        };
    }

    private String handleSend(CommandInput input, IPCManager ipc) {
        if (input.getArgCount() < 4) {
            return Formatter.red("Usage: ipc send <fromPid> <toPid> <message...>\n");
        }

        int fromPid = input.getIntArg(1, -1);
        int toPid   = input.getIntArg(2, -1);
        if (fromPid < 1 || toPid < 1) {
            return Formatter.red("Invalid PIDs.\n");
        }

        try {
            IPCMessage msg = ipc.sendDirect(fromPid, toPid, input.joinArgs(3));
            return Formatter.green("Message #" + msg.id() + " sent from P" + fromPid + " to P" + toPid + ".\n");
        } catch (IllegalArgumentException e) {
            return Formatter.red("IPC error: " + e.getMessage() + "\n");
        }
    }

    private String handleReceive(CommandInput input, IPCManager ipc) {
        if (input.getArgCount() < 2) {
            return Formatter.red("Usage: ipc recv <pid>\n");
        }
        int pid = input.getIntArg(1, -1);
        if (pid < 1) return Formatter.red("Invalid PID.\n");

        try {
            Optional<IPCMessage> opt = ipc.receiveDirect(pid);
            if (opt.isEmpty()) return Formatter.yellow("Inbox for P" + pid + " is empty.\n");
            return formatMessage("Received", opt.get());
        } catch (IllegalArgumentException e) {
            return Formatter.red("IPC error: " + e.getMessage() + "\n");
        }
    }

    private String handleInbox(CommandInput input, IPCManager ipc) {
        if (input.getArgCount() < 2) {
            return Formatter.red("Usage: ipc inbox <pid> [limit]\n");
        }
        int pid   = input.getIntArg(1, -1);
        int limit = input.getIntArg(2, 10);
        if (pid < 1) return Formatter.red("Invalid PID.\n");

        try {
            List<IPCMessage> messages = ipc.peekMailbox(pid, limit);
            return formatList("Mailbox P" + pid, messages);
        } catch (IllegalArgumentException e) {
            return Formatter.red("IPC error: " + e.getMessage() + "\n");
        }
    }

    private String handleCreateChannel(CommandInput input, IPCManager ipc) {
        if (input.getArgCount() < 2) {
            return Formatter.red("Usage: ipc mkchan <name>\n");
        }
        try {
            boolean created = ipc.createChannel(input.getArg(1));
            return created
                    ? Formatter.green("Channel created: " + input.getArg(1).toLowerCase() + "\n")
                    : Formatter.yellow("Channel already exists: " + input.getArg(1).toLowerCase() + "\n");
        } catch (IllegalArgumentException e) {
            return Formatter.red("IPC error: " + e.getMessage() + "\n");
        }
    }

    private String handlePublish(CommandInput input, IPCManager ipc) {
        if (input.getArgCount() < 4) {
            return Formatter.red("Usage: ipc publish <channel> <fromPid> <message...>\n");
        }
        String channel = input.getArg(1);
        int fromPid    = input.getIntArg(2, -1);
        if (fromPid < 1) return Formatter.red("Invalid PID.\n");

        try {
            IPCMessage msg = ipc.publish(channel, fromPid, input.joinArgs(3));
            return Formatter.green("Published message #" + msg.id() + " on channel '" + msg.channel() + "'.\n");
        } catch (IllegalArgumentException e) {
            return Formatter.red("IPC error: " + e.getMessage() + "\n");
        }
    }

    private String handleReadChannel(CommandInput input, IPCManager ipc) {
        if (input.getArgCount() < 2) {
            return Formatter.red("Usage: ipc readchan <channel> [limit]\n");
        }
        String channel = input.getArg(1);
        int limit      = input.getIntArg(2, 10);
        try {
            return formatList("Channel " + channel.toLowerCase(), ipc.peekChannel(channel, limit));
        } catch (IllegalArgumentException e) {
            return Formatter.red("IPC error: " + e.getMessage() + "\n");
        }
    }

    private String handleChannels(IPCManager ipc) {
        List<String> channels = ipc.listChannels();
        if (channels.isEmpty()) return Formatter.yellow("No named channels defined.\n");
        StringBuilder sb = new StringBuilder(Formatter.section("IPC Channels"));
        for (String channel : channels) {
            sb.append("  ").append(channel).append("\n");
        }
        return sb.toString();
    }

    private String handleInfo(IPCManager ipc, SimulationContext ctx) {
        StringBuilder sb = new StringBuilder(Formatter.section("IPC Status"));
        sb.append("  Registered processes : ").append(ctx.getProcessManager().size()).append("\n");
        sb.append("  Named channels       : ").append(ipc.listChannels().size()).append("\n");
        sb.append("  Pending messages     : ").append(ipc.getTotalPendingMessages()).append("\n");
        return sb.toString();
    }

    private String formatMessage(String title, IPCMessage msg) {
        return String.format(
                "\n%s\n  id=%d  time=%s\n  from=P%d  to=%s  channel=%s\n  payload: %s\n",
                Formatter.bold(title + " Message"),
                msg.id(),
                msg.timestampLabel(),
                msg.fromPid(),
                msg.toPid() == null ? "-" : "P" + msg.toPid(),
                msg.channel(),
                msg.payload());
    }

    private String formatList(String title, List<IPCMessage> messages) {
        StringBuilder sb = new StringBuilder(Formatter.section(title));
        if (messages.isEmpty()) {
            return sb.append(Formatter.yellow("  (empty)\n")).toString();
        }

        for (IPCMessage msg : messages) {
            sb.append(String.format("  #%d [%s] from P%d -> %s : %s\n",
                    msg.id(),
                    msg.timestampLabel(),
                    msg.fromPid(),
                    msg.toPid() == null ? msg.channel() : "P" + msg.toPid(),
                    msg.payload()));
        }
        return sb.toString();
    }

    @Override
    public String helpText() {
        return "ipc send <from> <to> <msg> | ipc recv <pid> | ipc inbox <pid> | ipc mkchan <name> | ipc publish <chan> <from> <msg>";
    }
}
