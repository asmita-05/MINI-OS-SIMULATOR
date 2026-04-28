package os.ipc;

import os.process.Process;
import os.process.ProcessManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Inter-process communication subsystem.
 *
 * Uses Java concurrent collections to model:
 * - per-process mailboxes (direct messages)
 * - named channels (publish / consume)
 */
public class IPCManager {

    private final ProcessManager processManager;
    private final AtomicLong     nextMessageId = new AtomicLong(1);
    private final Map<Integer, BlockingQueue<IPCMessage>> mailboxes = new ConcurrentHashMap<>();
    private final Map<String, BlockingQueue<IPCMessage>>  channels  = new ConcurrentHashMap<>();

    public IPCManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public void registerProcess(int pid) {
        mailboxes.computeIfAbsent(pid, ignored -> new LinkedBlockingQueue<>());
    }

    public void unregisterProcess(int pid) {
        mailboxes.remove(pid);
    }

    public IPCMessage sendDirect(int fromPid, int toPid, String payload) {
        ensurePid(fromPid);
        ensurePid(toPid);

        IPCMessage message = new IPCMessage(
                nextMessageId.getAndIncrement(),
                fromPid,
                toPid,
                "mailbox",
                payload,
                System.currentTimeMillis());

        mailboxes.computeIfAbsent(toPid, ignored -> new LinkedBlockingQueue<>()).offer(message);
        markSent(fromPid);
        return message;
    }

    public IPCMessage publish(String channel, int fromPid, String payload) {
        ensurePid(fromPid);
        String key = normalizeChannel(channel);
        IPCMessage message = new IPCMessage(
                nextMessageId.getAndIncrement(),
                fromPid,
                null,
                key,
                payload,
                System.currentTimeMillis());
        channels.computeIfAbsent(key, ignored -> new LinkedBlockingQueue<>()).offer(message);
        markSent(fromPid);
        return message;
    }

    public boolean createChannel(String channel) {
        String key = normalizeChannel(channel);
        return channels.putIfAbsent(key, new LinkedBlockingQueue<>()) == null;
    }

    public Optional<IPCMessage> receiveDirect(int pid) {
        ensurePid(pid);
        IPCMessage message = mailboxes.computeIfAbsent(pid, ignored -> new LinkedBlockingQueue<>()).poll();
        if (message != null) {
            markReceived(pid);
        }
        return Optional.ofNullable(message);
    }

    public List<IPCMessage> peekMailbox(int pid, int limit) {
        ensurePid(pid);
        return snapshot(mailboxes.computeIfAbsent(pid, ignored -> new LinkedBlockingQueue<>()), limit);
    }

    public List<IPCMessage> peekChannel(String channel, int limit) {
        String key = normalizeChannel(channel);
        return snapshot(channels.computeIfAbsent(key, ignored -> new LinkedBlockingQueue<>()), limit);
    }

    public Optional<IPCMessage> consumeChannel(String channel) {
        String key = normalizeChannel(channel);
        return Optional.ofNullable(channels.computeIfAbsent(key, ignored -> new LinkedBlockingQueue<>()).poll());
    }

    public Optional<IPCMessage> consumeChannel(String channel, int pid) {
        ensurePid(pid);
        Optional<IPCMessage> message = consumeChannel(channel);
        message.ifPresent(msg -> markReceived(pid));
        return message;
    }

    public int getPendingChannelCount(String channel) {
        String key = normalizeChannel(channel);
        return channels.computeIfAbsent(key, ignored -> new LinkedBlockingQueue<>()).size();
    }

    public int getPendingMailboxCount(int pid) {
        ensurePid(pid);
        return mailboxes.computeIfAbsent(pid, ignored -> new LinkedBlockingQueue<>()).size();
    }

    public int getTotalPendingMessages() {
        int total = 0;
        for (BlockingQueue<IPCMessage> queue : mailboxes.values()) total += queue.size();
        for (BlockingQueue<IPCMessage> queue : channels.values()) total += queue.size();
        return total;
    }

    public List<String> listChannels() {
        return new ArrayList<>(new TreeSet<>(channels.keySet()));
    }

    private List<IPCMessage> snapshot(BlockingQueue<IPCMessage> queue, int limit) {
        List<IPCMessage> items = new ArrayList<>(queue);
        if (items.isEmpty()) return Collections.emptyList();
        int end = Math.min(Math.max(limit, 1), items.size());
        return List.copyOf(items.subList(0, end));
    }

    private void ensurePid(int pid) {
        if (processManager.findByPid(pid).isEmpty()) {
            throw new IllegalArgumentException("Process P" + pid + " not found.");
        }
    }

    private String normalizeChannel(String channel) {
        String key = channel == null ? "" : channel.trim().toLowerCase();
        if (key.isEmpty()) throw new IllegalArgumentException("Channel name must not be blank.");
        return key;
    }

    private void markSent(int pid) {
        processManager.findByPid(pid).ifPresent(Process::incrementMessagesSent);
    }

    private void markReceived(int pid) {
        processManager.findByPid(pid).ifPresent(Process::incrementMessagesReceived);
    }
}
