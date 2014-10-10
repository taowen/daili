package org.github.taowen.daili;

import kilim.Pausable;
import kilim.PauseReason;
import kilim.Task;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

class SelectorBooking implements PauseReason, Comparable<SelectorBooking> {

    private final Queue<SelectorBooking> bookings;
    private final SelectionKey selectionKey;

    private long earliestDeadline = Long.MAX_VALUE;
    private long readDeadline = Long.MAX_VALUE;
    private Task readTask;
    private long writeDeadline = Long.MAX_VALUE;
    private Task writeTask;
    private long acceptDeadline = Long.MAX_VALUE;
    private Task acceptTask;
    private long connectDeadline = Long.MAX_VALUE;
    private Task connectTask;

    public SelectorBooking(Queue<SelectorBooking> bookings, SelectionKey selectionKey) {
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
        Task.pause(this);
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
        Task.pause(this);
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
        Task.pause(this);
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
        Task.pause(this);
        if (connectDeadline == -1) {
            connectUnblocked();
            throw new TimeoutException();
        }
    }

    public List<Task> ioUnblocked() {
        List<Task> tasks = new ArrayList<Task>();
        if (selectionKey.isAcceptable()) {
            Task taskUnblocked = acceptUnblocked();
            if (null == taskUnblocked) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_ACCEPT);
            } else {
                tasks.add(taskUnblocked);
            }
        }
        if (selectionKey.isConnectable()) {
            Task taskUnblocked = connectUnblocked();
            if (null == taskUnblocked) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_CONNECT);
            } else {
                tasks.add(taskUnblocked);
            }
        }
        if (selectionKey.isReadable()) {
            Task taskUnblocked = readUnblocked();
            if (null == taskUnblocked) {
                selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_READ);
            } else {
                tasks.add(taskUnblocked);
            }
        }
        if (selectionKey.isWritable()) {
            Task taskUnblocked = writeUnblocked();
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

    private Task readUnblocked() {
        Task unblocked = readTask;
        readDeadline = Long.MAX_VALUE;
        updateDeadline();
        readTask = null;
        return unblocked;
    }

    private Task connectUnblocked() {
        Task unblocked = connectTask;
        connectDeadline = Long.MAX_VALUE;
        updateDeadline();
        connectTask = null;
        return unblocked;
    }

    private Task acceptUnblocked() {
        Task unblocked = acceptTask;
        acceptDeadline = Long.MAX_VALUE;
        updateDeadline();
        acceptTask = null;
        return unblocked;
    }

    private Task writeUnblocked() {
        Task unblocked = writeTask;
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

    public long getEarliestDeadline() {
        return earliestDeadline;
    }

    public boolean cancelDeadTasks(long currentTimeMillis) {
        if (readDeadline > 0 && currentTimeMillis > readDeadline) {
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_READ);
            readDeadline = -1;
            updateDeadline();
            readTask.run();
            if (-1 == readDeadline) {
                throw new RuntimeException("read deadline unhandled");
            }
        }
        if (writeDeadline > 0 && currentTimeMillis > writeDeadline) {
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
            writeDeadline = -1;
            updateDeadline();
            writeTask.run();
            if (-1 == writeDeadline) {
                throw new RuntimeException("write deadline unhandled");
            }
        }
        if (acceptDeadline > 0 && currentTimeMillis > acceptDeadline) {
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_ACCEPT);
            acceptDeadline = -1;
            updateDeadline();
            acceptTask.run();
            if (-1 == acceptDeadline) {
                throw new RuntimeException("accept deadline unhandled");
            }
        }
        if (connectDeadline > 0 && currentTimeMillis > connectDeadline) {
            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_CONNECT);
            connectDeadline = -1;
            updateDeadline();
            connectTask.run();
            if (-1 == connectDeadline) {
                throw new AssertionError("connect deadline unhandled");
            }
        }
        if (0 == selectionKey.interestOps()) {
            selectionKey.cancel();
            return true;
        }
        return false;
    }

    @Override
    public int compareTo(SelectorBooking that) {
        long delta = that.earliestDeadline - this.earliestDeadline;
        if (delta > 0) {
            return -1;
        } else if (delta < 0) {
            return 1;
        }
        return 0;
    }

    public boolean hasPendingTask() {
        return Long.MAX_VALUE != earliestDeadline;
    }
}
