package org.github.taowen.daili;

import kilim.Pausable;
import kilim.PauseReason;
import kilim.Task;

import java.nio.channels.SelectableChannel;
import java.util.Queue;

class SelectorBooking implements PauseReason {
    private long readBlockedAt;
    private Task readTask;
    private long writeBlockedAt;
    private Task writeTask;
    private long acceptBlockedAt;
    private Task acceptTask;
    private long connectBlockedAt;
    private Task connectTask;

    public SelectorBooking(Queue<SelectorBooking> bookings) {
        bookings.offer(this);
    }

    public void readBlocked() throws Pausable {
        if (null != readTask) {
            throw new RuntimeException("multiple read blocked on same channel");
        }
        readBlockedAt = System.currentTimeMillis();
        readTask = Task.getCurrentTask();
        Task.pause(this);
    }

    public void writeBlocked() throws Pausable {
        if (null != writeTask) {
            throw new RuntimeException("multiple write blocked on same channel");
        }
        writeBlockedAt = System.currentTimeMillis();
        writeTask = Task.getCurrentTask();
        Task.pause(this);
    }

    public Task readUnblocked() {
        Task unblocked = readTask;
        readBlockedAt = 0;
        readTask = null;
        return unblocked;
    }

    public Task acceptUnblocked() {
        Task unblocked = acceptTask;
        acceptBlockedAt = 0;
        acceptTask = null;
        return unblocked;
    }

    public void acceptBlocked() throws Pausable {
        if (null != acceptTask) {
            throw new RuntimeException("multiple accept blocked on same channel");
        }
        acceptBlockedAt = System.currentTimeMillis();
        acceptTask = Task.getCurrentTask();
        Task.pause(this);
    }

    public Task writeUnblocked() {
        Task unblocked = writeTask;
        writeBlockedAt = 0;
        writeTask = null;
        return unblocked;
    }
}
