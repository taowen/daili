package org.github.taowen.daili;

import kilim.Pausable;
import kilim.PauseReason;
import kilim.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

class SelectorBooking extends Booking {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectorBooking.class);

    private final Queue<Booking> bookings;
    private final SelectionKey selectionKey;

    private long earliestDeadline = Long.MAX_VALUE;
    private long readDeadline = Long.MAX_VALUE;
    private Runnable readTask;
    private long writeDeadline = Long.MAX_VALUE;
    private Runnable writeTask;
    private long acceptDeadline = Long.MAX_VALUE;
    private Runnable acceptTask;
    private long connectDeadline = Long.MAX_VALUE;
    private Runnable connectTask;

    public SelectorBooking(Queue<Booking> bookings, SelectionKey selectionKey) {
        this.bookings = bookings;
        this.selectionKey = selectionKey;
    }

    public void readBlocked(long deadline) throws Pausable, TimeoutException {
        if (null != readTask) {
            throw new RuntimeException("multiple read blocked on same channel");
        }
        readDeadline = deadline;
        updateDeadline();
        readTask = Task.getCurrentTask();
        // =========== END SCHEDULER THREAD =========

        Task.pause(this);

        // =========== BEGIN WORKER THREAD =========
        if (readDeadline == -1) {
            readUnblocked();
            throw new TimeoutException();
        }
    }

    public void writeBlocked(long deadline) throws Pausable, TimeoutException {
        if (null != writeTask) {
            throw new RuntimeException("multiple write blocked on same channel");
        }
        writeDeadline = deadline;
        updateDeadline();
        writeTask = Task.getCurrentTask();
        // =========== END SCHEDULER THREAD =========

        Task.pause(this);

        // =========== BEGIN WORKER THREAD =========
        if (writeDeadline == -1) {
            writeUnblocked();
            throw new TimeoutException();
        }
    }

    public void acceptBlocked(long deadline) throws Pausable, TimeoutException {
        if (null != acceptTask) {
            throw new RuntimeException("multiple accept blocked on same channel");
        }
        acceptDeadline = deadline;
        updateDeadline();
        acceptTask = Task.getCurrentTask();
        // =========== END SCHEDULER THREAD =========

        Task.pause(this);

        // =========== BEGIN WORKER THREAD =========
        if (acceptDeadline == -1) {
            acceptUnblocked();
            throw new TimeoutException();
        }
    }

    public void connectBlocked(long deadline) throws Pausable, TimeoutException {
        if (null != connectTask) {
            throw new RuntimeException("multiple connect blocked on same channel");
        }
        connectDeadline = deadline;
        updateDeadline();
        connectTask = Task.getCurrentTask();
        // =========== END SCHEDULER THREAD =========

        Task.pause(this);

        // =========== BEGIN WORKER THREAD =========
        if (connectDeadline == -1) {
            connectUnblocked();
            throw new TimeoutException();
        }
    }

    public List<Runnable> ioUnblocked() {
        List<Runnable> tasks = new ArrayList<Runnable>();
        if (selectionKey.isAcceptable()) {
            Runnable taskUnblocked = acceptUnblocked();
            if (null == taskUnblocked) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_ACCEPT);
            } else {
                tasks.add(taskUnblocked);
            }
        }
        if (selectionKey.isConnectable()) {
            Runnable taskUnblocked = connectUnblocked();
            if (null == taskUnblocked) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_CONNECT);
            } else {
                tasks.add(taskUnblocked);
            }
        }
        if (selectionKey.isReadable()) {
            Runnable taskUnblocked = readUnblocked();
            if (null == taskUnblocked) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_READ);
            } else {
                tasks.add(taskUnblocked);
            }
        }
        if (selectionKey.isWritable()) {
            Runnable taskUnblocked = writeUnblocked();
            if (null == taskUnblocked) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            } else {
                tasks.add(taskUnblocked);
            }
        }
        if (0 == selectionKey.interestOps()) {
            selectionKey.cancel();
        }
        return tasks;
    }

    private Runnable readUnblocked() {
        Runnable unblocked = readTask;
        readDeadline = Long.MAX_VALUE;
        updateDeadline();
        readTask = null;
        return unblocked;
    }

    private Runnable connectUnblocked() {
        Runnable unblocked = connectTask;
        connectDeadline = Long.MAX_VALUE;
        updateDeadline();
        connectTask = null;
        return unblocked;
    }

    private Runnable acceptUnblocked() {
        Runnable unblocked = acceptTask;
        acceptDeadline = Long.MAX_VALUE;
        updateDeadline();
        acceptTask = null;
        return unblocked;
    }

    private Runnable writeUnblocked() {
        Runnable unblocked = writeTask;
        writeDeadline = Long.MAX_VALUE;
        updateDeadline();
        writeTask = null;
        return unblocked;
    }

    public void updateDeadline() {
        long originalValue = earliestDeadline;
        earliestDeadline = Long.MAX_VALUE;
        if (readDeadline > 0 && readDeadline < earliestDeadline) {
            earliestDeadline = readDeadline;
        }
        if (writeDeadline > 0 && writeDeadline < earliestDeadline) {
            earliestDeadline = writeDeadline;
        }
        if (acceptDeadline > 0 && acceptDeadline < earliestDeadline) {
            earliestDeadline = acceptDeadline;
        }
        if (connectDeadline > 0 && connectDeadline < earliestDeadline) {
            earliestDeadline = connectDeadline;
        }
        if (originalValue != earliestDeadline) {
            bookings.remove(this); // when timed out, the booking might be removed already
            if (earliestDeadline != Long.MAX_VALUE) {
                // add back in case read timed out, but write is still blocking
                if (!bookings.offer(this)) {
                    throw new RuntimeException("update booking failed");
                }
            }
        }
    }

    public long deadline() {
        return earliestDeadline;
    }

    public boolean cancelDeadTasks(long currentTimeMillis) {
        if (readDeadline > 0 && currentTimeMillis > readDeadline) {
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_READ);
            readDeadline = -1;
            updateDeadline();
            runTask(readTask);
        }
        if (writeDeadline > 0 && currentTimeMillis > writeDeadline) {
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            writeDeadline = -1;
            updateDeadline();
            runTask(writeTask);
        }
        if (acceptDeadline > 0 && currentTimeMillis > acceptDeadline) {
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_ACCEPT);
            acceptDeadline = -1;
            updateDeadline();
            runTask(acceptTask);
        }
        if (connectDeadline > 0 && currentTimeMillis > connectDeadline) {
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_CONNECT);
            connectDeadline = -1;
            updateDeadline();
            runTask(connectTask);
        }
        if (0 == selectionKey.interestOps()) {
            selectionKey.cancel();
            return true;
        }
        return false;
    }

    private void runTask(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            LOGGER.info("failed to run task: " + task, e);
        }
    }
}
