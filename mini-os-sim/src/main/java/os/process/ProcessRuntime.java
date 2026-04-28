package os.process;

/**
 * Live Java-thread runtime for a simulated process.
 *
 * The existing scheduling module remains an analytical simulator, while this
 * runtime gives the process-management module a concrete Java {@link Thread}
 * that can be started, paused, blocked, resumed, and terminated.
 */
public class ProcessRuntime implements Runnable {

    private static final long TICK_MS = 250L;

    private final Process process;
    private final Object  stateLock = new Object();
    private final Thread  worker;

    private volatile boolean alive = true;

    public ProcessRuntime(Process process) {
        this.process = process;
        this.worker  = new Thread(this, "os-proc-" + process.getPid() + "-" + process.getName());
        this.worker.setDaemon(true);
        this.process.bindJavaThread(worker);
        this.process.setCreatedAtMillis(System.currentTimeMillis());
        this.process.touch();
    }

    public void start() {
        worker.start();
        updateThreadState();
    }

    public void requestRun() {
        synchronized (stateLock) {
            if (!alive || process.getState() == ProcessState.TERMINATED) return;
            process.setState(ProcessState.RUNNING);
            process.touch();
            stateLock.notifyAll();
        }
        updateThreadState();
    }

    public void pause() {
        synchronized (stateLock) {
            if (!alive || process.getState() != ProcessState.RUNNING) return;
            process.setState(ProcessState.READY);
            process.touch();
        }
        updateThreadState();
    }

    public void block() {
        synchronized (stateLock) {
            if (!alive || process.getState() == ProcessState.TERMINATED) return;
            process.setState(ProcessState.WAITING);
            process.touch();
        }
        updateThreadState();
    }

    public void wakeUp() {
        synchronized (stateLock) {
            if (!alive || process.getState() == ProcessState.TERMINATED) return;
            process.setState(ProcessState.READY);
            process.touch();
            stateLock.notifyAll();
        }
        updateThreadState();
    }

    public void terminate() {
        alive = false;
        synchronized (stateLock) {
            process.setState(ProcessState.TERMINATED);
            process.touch();
            stateLock.notifyAll();
        }
        worker.interrupt();
        updateThreadState();
    }

    public boolean isAlive() {
        return alive && worker.isAlive();
    }

    public String getThreadState() {
        updateThreadState();
        return process.getJavaThreadState();
    }

    @Override
    public void run() {
        updateThreadState();
        try {
            while (alive) {
                waitUntilRunnable();
                if (!alive) break;

                process.incrementRuntimeTicks();
                updateThreadState();
                Thread.sleep(TICK_MS);
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            process.setJavaThreadState(worker.getState().name());
            if (process.getState() != ProcessState.TERMINATED) {
                process.setState(ProcessState.TERMINATED);
            }
            process.touch();
        }
    }

    private void waitUntilRunnable() throws InterruptedException {
        synchronized (stateLock) {
            while (alive && process.getState() != ProcessState.RUNNING) {
                updateThreadState();
                stateLock.wait(TICK_MS);
            }
        }
    }

    private void updateThreadState() {
        process.setJavaThreadState(worker.getState().name());
    }
}
