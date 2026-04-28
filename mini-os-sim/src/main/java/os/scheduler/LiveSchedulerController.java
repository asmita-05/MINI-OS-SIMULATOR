package os.scheduler;

import os.process.Process;
import os.process.ProcessManager;
import os.process.ProcessState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays a computed scheduling timeline against live Java-thread-backed
 * processes. The analytical scheduler remains the source of truth for the
 * chosen algorithm; this controller acts like the runtime dispatcher.
 */
public class LiveSchedulerController {

    private static final long UNIT_MS = 200L;

    private final ProcessManager processManager;
    private final Object replayLock = new Object();
    private volatile boolean running;
    private Thread replayThread;

    public LiveSchedulerController(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public boolean isRunning() {
        return running;
    }

    public void replayBlocking(ScheduleResult result) {
        Thread worker = createReplayThread(result, null);
        startReplay(worker);
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void replayAsync(ScheduleResult result, Runnable onComplete) {
        Thread worker = createReplayThread(result, onComplete);
        startReplay(worker);
    }

    private Thread createReplayThread(ScheduleResult result, Runnable onComplete) {
        return new Thread(() -> {
            try {
                replayTimeline(result);
            } finally {
                running = false;
                synchronized (replayLock) {
                    replayThread = null;
                }
                if (onComplete != null) onComplete.run();
            }
        }, "os-live-scheduler");
    }

    private void startReplay(Thread worker) {
        synchronized (replayLock) {
            if (running) {
                throw new IllegalStateException("A live scheduling replay is already running.");
            }
            running = true;
            replayThread = worker;
            worker.setDaemon(true);
            worker.start();
        }
    }

    private void replayTimeline(ScheduleResult result) {
        List<Process> live = processManager.getAll();
        if (live.isEmpty()) return;

        processManager.prepareForLiveScheduling();

        Map<Integer, Process> liveByPid = new HashMap<>();
        for (Process p : processManager.getAll()) liveByPid.put(p.getPid(), p);

        Map<Integer, Process> completedByPid = new HashMap<>();
        for (Process p : result.getCompletedProcesses()) completedByPid.put(p.getPid(), p);

        List<GanttEntry> gantt = result.getGanttChart();
        int ganttIndex = 0;

        for (int time = 0; time < result.getTotalTime(); time++) {
            while (ganttIndex < gantt.size() && time >= gantt.get(ganttIndex).getEndTime()) {
                ganttIndex++;
            }
            GanttEntry activeEntry = ganttIndex < gantt.size() ? gantt.get(ganttIndex) : GanttEntry.idle(time, time + 1);

            admitArrivals(liveByPid, time);
            applyDispatcherState(liveByPid, activeEntry, time);
            sleepUnit();
            finalizeTick(liveByPid, completedByPid, time + 1);
        }

        for (Process p : processManager.getAll()) {
            if (p.getState() != ProcessState.TERMINATED) {
                processManager.terminateProcess(p.getPid());
            }
        }
    }

    private void admitArrivals(Map<Integer, Process> liveByPid, int time) {
        for (Process p : liveByPid.values()) {
            if (p.getState() == ProcessState.NEW && p.getArrivalTime() <= time) {
                processManager.admit(p);
            }
        }
    }

    private void applyDispatcherState(Map<Integer, Process> liveByPid, GanttEntry activeEntry, int time) {
        int activePid = activeEntry.isIdle() ? GanttEntry.IDLE_PID : activeEntry.getPid();

        for (Process p : liveByPid.values()) {
            if (p.getState() == ProcessState.TERMINATED || p.getArrivalTime() > time) continue;

            if (p.getPid() == activePid) {
                p.recordFirstScheduled(time);
                processManager.startProcess(p.getPid());
                p.setRemainingTime(Math.max(0, p.getRemainingTime() - 1));
            } else if (p.getState() == ProcessState.RUNNING) {
                processManager.pauseProcess(p.getPid());
            }
        }
    }

    private void finalizeTick(Map<Integer, Process> liveByPid,
                              Map<Integer, Process> completedByPid,
                              int endOfTick) {
        for (Process p : liveByPid.values()) {
            Process completed = completedByPid.get(p.getPid());
            if (completed == null) continue;

            if (completed.getCompletionTime() == endOfTick) {
                p.setCompletionTime(endOfTick);
                p.setRemainingTime(0);
                processManager.terminateProcess(p.getPid());
            }
        }
    }

    private void sleepUnit() {
        try {
            Thread.sleep(UNIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
