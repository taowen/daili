package org.github.taowen.daili;

import kilim.Pausable;
import kilim.PauseReason;
import kilim.Task;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

class SelectorBooking implements PauseReason, Comparable<SelectorBooking> {

    private final Queue<SelectorBooking> bookings;
    private final SelectionKey selectionKey;

    private long earliestDeadline = Long.MAX_VALUE;
    private long readDeadline;
    private Task readTask;
    private long writeDeadline;
    private Task writeTask;
    private long acceptDeadline;
    private Task acceptTask;
    private long connectDeadline;
    private Task connectTask;

    public SelectorBooking(Queue<SelectorBooking> bookings, SelectionKey selectionKey) {
        this.bookings = bookings;
        this.selectionKey = selectionKey;
        if (!bookings.offer(this)) {
            throw new RuntimeException("add booking failed");
        }
    }

    public void readBlocked(long deadline) throws Pausable {
        if (null != readTask) {
            throw new RuntimeException("multiple read blocked on same channel");
        }
        readDeadline = deadline;
        updateDeadline();
        readTask = Task.getCurrentTask();
        Task.pause(this);
        if (readDeadline == -1) {
            readUnblocked();
            throw new RuntimeException("timeout");
        }
    }

    public void writeBlocked(long deadline) throws Pausable {
        if (null != writeTask) {
            throw new RuntimeException("multiple write blocked on same channel");
        }
        writeDeadline = deadline;
        updateDeadline();
        writeTask = Task.getCurrentTask();
        Task.pause(this);
        if (writeDeadline == -1) {
            writeUnblocked();
            throw new RuntimeException("timeout");
        }
    }

    public void acceptBlocked(long deadline) throws Pausable {
        if (null != acceptTask) {
            throw new RuntimeException("multiple accept blocked on same channel");
        }
        acceptDeadline = deadline;
        updateDeadline();
        acceptTask = Task.getCurrentTask();
        Task.pause(this);
        if (acceptDeadline == -1) {
            acceptUnblocked();
            throw new RuntimeException("timeout");
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
        return tasks;
    }

    private Task readUnblocked() {
        Task unblocked = readTask;
        readDeadline = 0;
        updateDeadline();
        readTask = null;
        return unblocked;
    }

    private Task acceptUnblocked() {
        Task unblocked = acceptTask;
        acceptDeadline = 0;
        updateDeadline();
        acceptTask = null;
        return unblocked;
    }

    private Task writeUnblocked() {
        Task unblocked = writeTask;
        writeDeadline = 0;
        updateDeadline();
        writeTask = null;
        return unblocked;
    }

    public void updateDeadline() {
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
        bookings.remove(this);
        if (!bookings.offer(this)) {
            throw new RuntimeException("update booking failed");
        }
    }

    public long getEarliestDeadline() {
        return earliestDeadline;
    }

    public void cancelDeadTasks(long currentTimeMillis) {
        if (null != readTask && currentTimeMillis > readDeadline) {
            readDeadline = -1;
            readTask.resume();
            if (-1 == readDeadline) {
                throw new RuntimeException("read deadline unhandled");
            }
        }
        if (null != writeTask && currentTimeMillis > writeDeadline) {
            writeDeadline = -1;
            writeTask.resume();
            if (-1 == writeDeadline) {
                throw new RuntimeException("write deadline unhandled");
            }
        }
        if (null != acceptTask && currentTimeMillis > acceptDeadline) {
            acceptDeadline = -1;
            acceptTask.resume();
            if (-1 == acceptDeadline) {
                throw new RuntimeException("accept deadline unhandled");
            }
        }
        if (null != connectTask && currentTimeMillis > connectDeadline) {
            connectDeadline = -1;
            connectTask.resume();
            if (-1 == connectDeadline) {
                throw new AssertionError("connect deadline unhandled");
            }
        }
    }

    @Override
    public int compareTo(SelectorBooking that) {
        return (int)(earliestDeadline - that.earliestDeadline);
    }
}
